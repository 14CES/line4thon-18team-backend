package com.dotlo.line4thon_18team.place.takeout;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.place.importer.ImportedList;
import com.dotlo.line4thon_18team.place.importer.ImportedPlace;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Google Takeout "저장됨" 파일(CSV 하나 또는 Takeout zip)을 읽어서 리스트별 장소 목록으로 바꾼다.
 *
 * Takeout CSV 모양 (파일 1개 = 구글맵 리스트 1개, 계정 언어에 따라 헤더가 영어/한국어):
 *   제목,메모,URL,태그,댓글
 *   (빈 줄)
 *   동국대학교,동국대애💛,https://www.google.com/maps/place/.../data=!4m2!3m1!1s0x357ca3...:0x809f...,,
 *
 * 장소 판별:
 *   - URL 에 feature ID(0x...:0x...)가 있으면 그걸로 저장
 *   - 없으면(직접 찍은 핀 등) URL 의 좌표로 저장
 *   - 둘 다 없으면 건너뛴다
 */
@Component
public class TakeoutParser {

    private static final String UTF8_BOM = "﻿";
    private static final long MAX_UNZIPPED_BYTES = 50L * 1024 * 1024;   // 압축 폭탄 방지

    private static final List<String> TITLE_COLUMNS = List.of("title", "제목");
    private static final List<String> NOTE_COLUMNS = List.of("note", "메모");
    private static final List<String> URL_COLUMNS = List.of("url");

    // 예) 0x357ca302d491f811:0x809ffb62a47609b5  (':' 가 %3A 로 인코딩돼 있어도 허용)
    private static final Pattern FEATURE_ID = Pattern.compile("(0x[0-9a-fA-F]+)(?::|%3[aA])(0x[0-9a-fA-F]+)");
    // 예) /maps/search/34.6687,135.5013  ·  ?q=34.6687,135.5013  ·  @34.6687,135.5013
    private static final Pattern COORDINATES =
            Pattern.compile("(?:/search/|[?&]q=|@)(-?\\d{1,2}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)");

    public List<ImportedList> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        List<ImportedList> lists;
        if (filename.endsWith(".csv")) {
            lists = List.of(parseCsv(file.getOriginalFilename(), readBytes(file), true));
        } else if (filename.endsWith(".zip")) {
            lists = parseZip(file);
        } else {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        if (lists.stream().allMatch(list -> list.places().isEmpty())) {
            throw new BusinessException(ErrorCode.NO_SAVED_PLACES);
        }
        return lists;
    }

    // ---------- zip ----------

    // zip 안의 CSV 를 전부 읽는다. Takeout 형식이 아닌 CSV(다른 서비스 데이터)는 건너뛴다.
    private List<ImportedList> parseZip(MultipartFile file) {
        List<ImportedList> lists = new ArrayList<>();
        long totalBytes = 0;

        try (ZipInputStream zip = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".csv")) {
                    continue;
                }
                byte[] bytes = readEntry(zip, MAX_UNZIPPED_BYTES - totalBytes);
                totalBytes += bytes.length;

                ImportedList list = parseCsv(entry.getName(), bytes, false);
                if (list != null) {
                    lists.add(list);
                }
            }
        } catch (ZipException e) {
            throw new BusinessException(ErrorCode.INVALID_ZIP_FILE);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 파일을 읽지 못했습니다.", e);
        }

        if (lists.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_CSV_IN_ZIP);
        }
        return lists;
    }

    private byte[] readEntry(InputStream in, long remaining) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (out.size() + read > remaining) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    // ---------- CSV ----------

    /**
     * @param strict true 면 Takeout 형식이 아닐 때 에러, false(zip 안의 파일)면 null 을 돌려줘서 건너뛴다
     */
    private ImportedList parseCsv(String path, byte[] bytes, boolean strict) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.startsWith(UTF8_BOM)) {
            text = text.substring(1);   // 엑셀 등이 붙이는 BOM 이 있으면 첫 열 이름이 "﻿제목" 이 돼서 제거
        }

        List<List<String>> rows = CsvReader.read(text);
        Map<String, Integer> header = rows.isEmpty() ? Map.of() : readHeader(rows.get(0));
        int titleIndex = findColumn(header, TITLE_COLUMNS);
        int urlIndex = findColumn(header, URL_COLUMNS);
        int noteIndex = findColumn(header, NOTE_COLUMNS);   // 메모 열은 없어도 허용

        if (titleIndex < 0 || urlIndex < 0) {
            if (strict) {
                throw new BusinessException(ErrorCode.INVALID_CSV_FORMAT);
            }
            return null;
        }

        List<ImportedPlace> places = new ArrayList<>();
        int skippedRows = 0;
        for (List<String> row : rows.subList(1, rows.size())) {
            if (row.stream().allMatch(String::isBlank)) {
                continue;   // Takeout CSV 는 헤더 바로 다음에 빈 줄이 있다
            }
            ImportedPlace place = toPlace(cell(row, titleIndex), cell(row, noteIndex), cell(row, urlIndex));
            if (place == null) {
                skippedRows++;
            } else {
                places.add(place);
            }
        }
        return new ImportedList(toListName(path), places, skippedRows);
    }

    private ImportedPlace toPlace(String name, String note, String url) {
        String noteOrNull = note.isEmpty() ? null : note;

        Matcher featureId = FEATURE_ID.matcher(url);
        if (featureId.find() && !name.isEmpty()) {
            return new ImportedPlace(name, noteOrNull, url, featureId.group(1) + ":" + featureId.group(2), null, null);
        }

        Matcher coordinates = COORDINATES.matcher(URLDecoder.decode(url, StandardCharsets.UTF_8));
        if (coordinates.find()) {
            double lat = Double.parseDouble(coordinates.group(1));
            double lng = Double.parseDouble(coordinates.group(2));
            String placeName = name.isEmpty() ? lat + ", " + lng : name;   // 이름 없는 핀은 좌표를 이름으로
            return new ImportedPlace(placeName, noteOrNull, url, null, lat, lng);
        }
        return null;
    }

    // 열 이름 → 몇 번째 열인지. 대소문자·앞뒤 공백 무시
    private Map<String, Integer> readHeader(List<String> headerRow) {
        Map<String, Integer> header = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            header.putIfAbsent(headerRow.get(i).trim().toLowerCase(), i);
        }
        return header;
    }

    private int findColumn(Map<String, Integer> header, List<String> candidates) {
        for (String name : candidates) {
            Integer index = header.get(name);
            if (index != null) {
                return index;
            }
        }
        return -1;
    }

    // 열이 모자란 행도 있을 수 있어서, 없는 칸은 빈 문자열로 본다
    private String cell(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return "";
        }
        return row.get(index).trim();
    }

    // "Takeout/Save/4호선톤1.csv" → "4호선톤1"
    private String toListName(String path) {
        String filename = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
        return filename.substring(0, filename.length() - ".csv".length()).trim();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 파일을 읽지 못했습니다.", e);
        }
    }
}

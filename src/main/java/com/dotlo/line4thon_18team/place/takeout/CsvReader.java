package com.dotlo.line4thon_18team.place.takeout;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV 텍스트를 "행 목록 → 칸 목록" 으로 쪼갠다.
 *
 * 쉼표로 단순 split 하면 안 되는 이유:
 * 메모에 쉼표·줄바꿈·따옴표가 들어가면 구글이 그 칸을 "..." 로 감싸서 내보내기 때문.
 *   예) 경복궁,"야경 예쁨, 꼭 가기",https://...
 *   예) "따옴표 ""안"" 글자"  → 따옴표 안의 "" 는 실제 따옴표 1개
 */
final class CsvReader {

    private CsvReader() {
    }

    static List<List<String>> read(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inQuotes) {
                if (c != '"') {
                    cell.append(c);                       // 따옴표 안에서는 쉼표·줄바꿈도 그냥 글자
                } else if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');                     // "" → " 한 개
                    i++;
                } else {
                    inQuotes = false;                     // 따옴표 칸 끝
                }
                continue;
            }

            switch (c) {
                case '"' -> inQuotes = true;
                case ',' -> {
                    row.add(cell.toString());
                    cell.setLength(0);
                }
                case '\n' -> {
                    row.add(cell.toString());
                    cell.setLength(0);
                    rows.add(row);
                    row = new ArrayList<>();
                }
                case '\r' -> {
                    // 윈도우 줄바꿈(\r\n)의 \r 은 무시
                }
                default -> cell.append(c);
            }
        }

        if (inQuotes) {
            throw new BusinessException(ErrorCode.INVALID_CSV_FORMAT, "CSV 따옴표가 닫히지 않았습니다.");
        }

        // 파일 끝에 줄바꿈이 없을 때 마지막 행 처리
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }
}

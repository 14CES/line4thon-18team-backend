package com.dotlo.line4thon_18team.place.importer;

import java.util.List;

/**
 * 구글맵 리스트 하나 (Takeout CSV 파일 1개, 또는 공유 리스트 링크 1개).
 *
 * @param listName    리스트 이름
 * @param places      읽은 장소들
 * @param skippedRows 장소를 알아낼 수 없어서 건너뛴 행 수
 */
public record ImportedList(String listName, List<ImportedPlace> places, int skippedRows) {
}

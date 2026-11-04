package com.dotlo.line4thon_18team.place.takeout;

import java.util.List;

/**
 * Takeout CSV 파일 하나 = 구글맵 리스트 하나.
 *
 * @param listName    리스트 이름 (파일 이름에서 .csv 를 뺀 것)
 * @param places      읽은 장소들
 * @param skippedRows feature ID 도 좌표도 없어서 건너뛴 행 수
 */
public record TakeoutList(String listName, List<TakeoutPlace> places, int skippedRows) {
}

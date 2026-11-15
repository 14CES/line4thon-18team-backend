package com.dotlo.line4thon_18team.chat.poll.repository;

import com.dotlo.line4thon_18team.chat.poll.entity.TripPollCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TripPollCandidateRepository extends JpaRepository<TripPollCandidate, Long> {

    List<TripPollCandidate> findByPollIdInOrderByPollIdAscSortOrderAsc(Collection<Long> pollIds);
}

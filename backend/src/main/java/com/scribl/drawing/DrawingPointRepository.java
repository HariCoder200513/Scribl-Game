package com.scribl.drawing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DrawingPointRepository extends JpaRepository<DrawingPoint, Long> {

    List<DrawingPoint> findByRoundIdAndSeqGreaterThanOrderBySeqAsc(Long roundId, long sinceSeq);

    /** Highest seq already assigned in a round; 0 when the round has no events. */
    @Query("select coalesce(max(p.seq), 0) from DrawingPoint p where p.roundId = :roundId")
    Long maxSeq(@Param("roundId") Long roundId);
}
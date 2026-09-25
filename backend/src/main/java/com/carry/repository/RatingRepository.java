package com.carry.repository;

import com.carry.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByRatedUserId(Long ratedUserId);
    Optional<Rating> findByRequestId(Long requestId);
    boolean existsByRequestId(Long requestId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.ratedUser.id = :ratedUserId")
    Double findAverageScoreByRatedUserId(@Param("ratedUserId") Long ratedUserId);
}

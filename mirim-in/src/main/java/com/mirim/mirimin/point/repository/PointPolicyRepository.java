package com.mirim.mirimin.point.repository;

import com.mirim.mirimin.point.entity.PointAction;
import com.mirim.mirimin.point.entity.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

    Optional<PointPolicy> findByAction(PointAction action);
}

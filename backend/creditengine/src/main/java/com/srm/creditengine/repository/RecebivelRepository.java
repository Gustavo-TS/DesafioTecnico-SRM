package com.srm.creditengine.repository;

import com.srm.creditengine.model.Recebivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecebivelRepository extends JpaRepository<Recebivel, UUID> {
}
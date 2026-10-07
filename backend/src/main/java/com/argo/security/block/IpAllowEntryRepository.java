package com.argo.security.block;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IpAllowEntryRepository extends JpaRepository<IpAllowEntry, String> {
}

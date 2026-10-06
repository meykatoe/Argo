package com.argo.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IpAllowEntryRepository extends JpaRepository<IpAllowEntry, String> {
}

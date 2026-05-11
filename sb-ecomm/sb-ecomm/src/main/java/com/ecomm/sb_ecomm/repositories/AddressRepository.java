package com.ecomm.sb_ecomm.repositories;

import com.ecomm.sb_ecomm.models.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
}

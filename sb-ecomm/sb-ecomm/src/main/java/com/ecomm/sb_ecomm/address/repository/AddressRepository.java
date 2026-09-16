package com.ecomm.sb_ecomm.address.repository;

import com.ecomm.sb_ecomm.address.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByUsersIdAndIsDefaultTrue(Long usersId);

    Optional<Address> findByUsersIdAndId(Long usersId, Long id);

}

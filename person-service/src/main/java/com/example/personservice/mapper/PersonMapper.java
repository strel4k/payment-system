package com.example.personservice.mapper;

import com.example.dto.person.AddressResponse;
import com.example.dto.person.IndividualResponse;
import com.example.dto.person.PersonResponse;
import com.example.personservice.entity.AddressEntity;
import com.example.personservice.entity.IndividualEntity;
import com.example.personservice.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {

    public PersonResponse toResponse(UserEntity u) {
        var res = new PersonResponse()
                .userId(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .filled(u.getFilled());
        res.address(toAddress(u.getAddress()));
        res.individual(toIndividual(u.getIndividual()));

        return res;
    }

    private AddressResponse toAddress(AddressEntity a) {
        if (a == null) return null;
        var resp = new AddressResponse()
                .id(a.getId())
                .address(a.getAddress())
                .zipCode(a.getZipCode())
                .city(a.getCity())
                .state(a.getState());
        if (a.getCountry() != null) {
            resp.countryId(a.getCountry().getId());
        }
        return resp;
    }

    private IndividualResponse toIndividual(IndividualEntity i) {
        if (i == null) return null;
        return new IndividualResponse()
                .id(i.getId())
                .passportNumber(i.getPassportNumber())
                .phoneNumber(i.getPhoneNumber())
                .status(i.getStatus());
    }
}
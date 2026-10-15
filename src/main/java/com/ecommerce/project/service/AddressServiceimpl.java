package com.ecommerce.project.service;

import com.ecommerce.project.exception.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositries.AddressRepository;
import com.ecommerce.project.repositries.UserRepository;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceimpl implements AddressService {
    @Autowired
    private  ModelMapper modelMapper;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private UserRepository userRepository;


    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);

        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);
        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addresses = addressRepository.findAll();
        List<AddressDTO> addressDTOS =   addresses.stream().map(address -> modelMapper.map(address, AddressDTO.class)).collect(Collectors.toList());
        return addressDTOS;

    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address =  addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","AddressId",addressId));
        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAllAddressesByUser() {
        User user = authUtil.loggerInUser();
        List<Address> addresses = user.getAddresses();
        List<AddressDTO> addressDTOS = addresses.stream().map(address -> modelMapper.map(address, AddressDTO.class)).collect(Collectors.toList());
        return addressDTOS;

    }

    @Override
    public AddressDTO updateAddressById(Long addressId, AddressDTO addressDTO) {
        Address address = addressRepository.findById(addressId).orElseThrow(() -> new ResourceNotFoundException("Address","AddressId",addressId));

        address.setStreet(addressDTO.getStreet());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setBuildingName(addressDTO.getBuildingName());
        address.setCountry(addressDTO.getCountry());
        address.setPincode(addressDTO.getPincode());

        Address updatedaddress = addressRepository.save(address);

        User user = address.getUser();
        user.getAddresses().removeIf(add -> add.getAddressId().equals(addressId));
        user.getAddresses().add(updatedaddress);

        AddressDTO updatedAddressDTO = modelMapper.map(updatedaddress, AddressDTO.class);
        return  updatedAddressDTO;

    }

    @Override
    public String deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow(() -> new ResourceNotFoundException("Address","AddressId",addressId));
        User user = address.getUser();
        user.getAddresses().removeIf(add -> add.getAddressId().equals(addressId));
        userRepository.save(user);
        addressRepository.delete(address);
        return "Address deleted successfully";

    }

}

package com.appsdeveloperblog.app.ws.service.impl;

import com.appsdeveloperblog.app.ws.exceptions.UserServiceException;
import com.appsdeveloperblog.app.ws.io.entity.AddressEntity;
import com.appsdeveloperblog.app.ws.io.entity.UserEntity;
import com.appsdeveloperblog.app.ws.io.repository.PasswordResetTokenRepository;
import com.appsdeveloperblog.app.ws.io.repository.UserRepository;
import com.appsdeveloperblog.app.ws.service.UserService;
import com.appsdeveloperblog.app.ws.shared.AmazonSES;
import com.appsdeveloperblog.app.ws.shared.Utils;
import com.appsdeveloperblog.app.ws.shared.dto.AddressDto;
import com.appsdeveloperblog.app.ws.shared.dto.UserDto;
import com.appsdeveloperblog.app.ws.ui.model.response.ErrorMessages;


import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final int ADDRESS_LENGTH = 30;
    private static final int PUBLIC_USERID_LENGTH = 30;

    private final UserRepository userRepository;
    private final Utils utils;
    private final BCryptPasswordEncoder encoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AmazonSES amazonSES;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, Utils utils, BCryptPasswordEncoder encoder,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           AmazonSES amazonSES) {
        this.userRepository = userRepository;
        this.utils = utils;
        this.encoder = encoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.amazonSES = amazonSES;
    }

    @Override
    public UserDto createUser(UserDto user) {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new UserServiceException(
                    ErrorMessages.RECORD_ALREADY_EXISTS.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        List<AddressDto> addressDtoList = user.getAddresses();
        if (!addressDtoList.isEmpty()) {
            user.getAddresses().forEach(addressDto -> {
                addressDto.setUserDetails(user);
                addressDto.setAddressId(utils.generateAddressId(ADDRESS_LENGTH));
            });
        }
        ModelMapper modelMapper = new ModelMapper();
        UserEntity userEntity = modelMapper.map(user, UserEntity.class);
        String publicUserId = utils.generateUserId(PUBLIC_USERID_LENGTH);
        userEntity.setUserId(publicUserId);
        userEntity.setEncryptedPassword(encoder.encode(user.getPassword()));
        userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));
        UserEntity storedUserDetails = userRepository.save(userEntity);
        //amazonSES.verifyEmail(returnValue);
        return modelMapper.map(storedUserDetails, UserDto.class);
    }

    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            throw new UserServiceException(
                    ErrorMessages.NO_RECORD_FOUND.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        UserDto returnValue = new UserDto();
        BeanUtils.copyProperties(userEntity, returnValue);
        return returnValue;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        UserDto returnValue = new UserDto();
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) {
            throw new UserServiceException(
                    ErrorMessages.NO_RECORD_FOUND.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        List<AddressEntity> addressEntityList = userEntity.getAddresses();
        if (!addressEntityList.isEmpty()) {
            copyAddressEntityListIntoAddressDtoList(addressEntityList, returnValue.getAddresses());
        }
        BeanUtils.copyProperties(userEntity, returnValue);
        return returnValue;
    }

    @Override
    public UserDto updateUser(String userId, UserDto user) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) {
            throw new UserServiceException(
                    ErrorMessages.NO_RECORD_FOUND.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        UserEntity updatedUserDetails = userRepository.save(userEntity);
        return  new ModelMapper().map(updatedUserDetails, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {

    }

    @Override
    public List<UserDto> getUsers(int page, int limit) {
        return null;
    }

    @Override
    public boolean verifyEmailToken(String token) {
        return false;
    }

    @Override
    public boolean requestPasswordReset(String email) {
        return false;
    }

    @Override
    public boolean resetPassword(String token, String password) {
        return false;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            throw new UsernameNotFoundException(email);
        }
        User user = new User(
                userEntity.getEmail(),
                userEntity.getEncryptedPassword(),
                true,
                true,
                true,
                true,
                new ArrayList<>()
        );
        return user;
    }

    private void copyAddressEntityListIntoAddressDtoList(List<AddressEntity> addressEntityList,
                                                         List<AddressDto> addressDtoList) {
        for (AddressEntity addressEntity : addressEntityList) {
            AddressDto addressDto = new AddressDto();
            BeanUtils.copyProperties(addressEntity, addressDto);
            addressDtoList.add(addressDto);
        }
    }

}

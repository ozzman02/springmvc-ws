package com.appsdeveloperblog.app.ws.service.impl;

import com.appsdeveloperblog.app.ws.exceptions.ServiceException;
import com.appsdeveloperblog.app.ws.io.entity.UserEntity;
import com.appsdeveloperblog.app.ws.io.repository.PasswordResetTokenRepository;
import com.appsdeveloperblog.app.ws.io.repository.UserRepository;
import com.appsdeveloperblog.app.ws.service.UserService;
import com.appsdeveloperblog.app.ws.shared.AmazonSES;
import com.appsdeveloperblog.app.ws.shared.Utils;
import com.appsdeveloperblog.app.ws.shared.dto.AddressDto;
import com.appsdeveloperblog.app.ws.shared.dto.UserDto;
import com.appsdeveloperblog.app.ws.ui.model.response.ErrorMessages;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
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
    public UserDto createUser(UserDto userDto) {
        if (userRepository.findByEmail(userDto.getEmail()) != null) {
            throw new ServiceException(
                    ErrorMessages.RECORD_ALREADY_EXISTS.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        return buildUserDto(userDto);
    }

    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            throw new ServiceException(
                    ErrorMessages.NO_RECORD_FOUND.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ModelMapper().map(userEntity, UserDto.class);
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) {
            throw new ServiceException(
                    ErrorMessages.NO_RECORD_FOUND.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ModelMapper().map(userEntity, UserDto.class);
    }

    @Override
    public UserDto updateUser(String userId, UserDto user) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) {
            throw new ServiceException(
                    ErrorMessages.NO_RECORD_FOUND.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        UserEntity updatedUserDetails = userRepository.save(userEntity);
        return new ModelMapper().map(updatedUserDetails, UserDto.class);
    }

    @Transactional
    @Override
    public void deleteUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) {
            throw new ServiceException(
                    ErrorMessages.NO_RECORD_FOUND.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }
        userRepository.delete(userEntity);
    }

    @Override
    public List<UserDto> getUsers(int page, int limit) {
        if (page > 0) page = page - 1;
        Pageable pageableRequest = PageRequest.of(page, limit);
        Page<UserEntity> usersPage = userRepository.findAll(pageableRequest);
        List<UserEntity> users = usersPage.getContent();
        Type userDtoListType = new TypeToken<List<UserDto>>() {}.getType();
        return new ModelMapper().map(users, userDtoListType);
    }

    @Override
    public boolean verifyEmailToken(String token) {
        boolean isVerified = false;
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);
        if (userEntity != null) {
            boolean hasTokenExpired = Utils.hasTokenExpired(token);
            if (!hasTokenExpired) {
                userEntity.setEmailVerificationToken(null);
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                isVerified = true;
            }
        }
        return isVerified;
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
        return new User(
                userEntity.getEmail(),
                userEntity.getEncryptedPassword(),
                userEntity.getEmailVerificationStatus(),
                true,
                true,
                true,
                new ArrayList<>()
        );
    }

    private UserDto buildUserDto(UserDto userDto) {
        List<AddressDto> addressDtoList = userDto.getAddresses();
        if (!addressDtoList.isEmpty()) {
            userDto.getAddresses().forEach(addressDto -> {
                addressDto.setUserDetails(userDto);
                addressDto.setAddressId(utils.generateAddressId(ADDRESS_LENGTH));
            });
        }
        ModelMapper modelMapper = new ModelMapper();
        UserEntity userEntity = modelMapper.map(userDto, UserEntity.class);
        String publicUserId = utils.generateUserId(PUBLIC_USERID_LENGTH);
        userEntity.setUserId(publicUserId);
        userEntity.setEncryptedPassword(encoder.encode(userDto.getPassword()));
        userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));
        userEntity.setEmailVerificationStatus(false);
        UserEntity storedUserEntity = userRepository.save(userEntity);
        //amazonSES.verifyEmail(returnValue);
        return modelMapper.map(storedUserEntity, UserDto.class);
    }

}

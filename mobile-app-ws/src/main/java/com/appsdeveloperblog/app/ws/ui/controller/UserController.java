package com.appsdeveloperblog.app.ws.ui.controller;

import com.appsdeveloperblog.app.ws.service.AddressService;
import com.appsdeveloperblog.app.ws.service.UserService;
import com.appsdeveloperblog.app.ws.shared.dto.AddressDto;
import com.appsdeveloperblog.app.ws.shared.dto.UserDto;
import com.appsdeveloperblog.app.ws.ui.model.request.PasswordResetModel;
import com.appsdeveloperblog.app.ws.ui.model.request.PasswordResetRequestModel;
import com.appsdeveloperblog.app.ws.ui.model.request.UserDetailsRequestModel;
import com.appsdeveloperblog.app.ws.ui.model.response.AddressResource;
import com.appsdeveloperblog.app.ws.ui.model.response.OperationStatusModel;
import com.appsdeveloperblog.app.ws.ui.model.response.RequestOperationStatus;
import com.appsdeveloperblog.app.ws.ui.model.response.UserResource;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


@RestController
@RequestMapping("/users")
public class UserController {

    private static final String USER_LINK_NAME = "user";

    private static final String ADDRESSES_LINK_NAME = "addresses";

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    /*@ApiOperation(
            value = "The Get User Details Web Service Endpoint",
            notes = "${userController.GetUser.ApiOperation.Notes}"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name="authorization",
                    value="${userController.authorizationHeader.description}",
                    paramType="header"
            )
    })*/
    @GetMapping(
            path = "/{id}",
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public UserResource getUser(@PathVariable String id) {
        UserDto userDto = userService.getUserByUserId(id);
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(userDto, UserResource.class);
    }

    @PostMapping(
            consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public UserResource createUser(@RequestBody UserDetailsRequestModel userDetails) {
        ModelMapper modelMapper = new ModelMapper();
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        UserDto createdUser = userService.createUser(userDto);
        return modelMapper.map(createdUser, UserResource.class);
    }

    /*@ApiImplicitParams({
            @ApiImplicitParam(
                    name="authorization",
                    value="${userController.authorizationHeader.description}",
                    paramType="header"
            )
    })*/
    @PutMapping(
            path = "/{id}",
            consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public UserResource updateUser(@PathVariable String id, @RequestBody UserDetailsRequestModel userDetails) {
        UserDto userDto = new ModelMapper().map(userDetails, UserDto.class);
        UserDto updateUser = userService.updateUser(id, userDto);
        return new ModelMapper().map(updateUser, UserResource.class);
    }

    /*@ApiImplicitParams({
            @ApiImplicitParam(
                    name="authorization",
                    value="${userController.authorizationHeader.description}",
                    paramType="header"
            )
    })*/
    @DeleteMapping(
            path = "/{id}",
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public OperationStatusModel deleteUser(@PathVariable String id) {
        OperationStatusModel returnValue = new OperationStatusModel();
        returnValue.setOperationName(RequestOperationName.DELETE.name());
        userService.deleteUser(id);
        returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        return returnValue;
    }

    /*@ApiImplicitParams({
            @ApiImplicitParam(
                    name="authorization",
                    value="${userController.authorizationHeader.description}",
                    paramType="header"
            )
    })*/
    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public List<UserResource> getUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "limit", defaultValue = "2") int limit) {
        List<UserDto> users = userService.getUsers(page, limit);
        Type listType = new TypeToken<List<UserResource>>() {}.getType();
        return new ModelMapper().map(users, listType);
    }

    /*@ApiImplicitParams({
            @ApiImplicitParam(
                    name="authorization",
                    value="${userController.authorizationHeader.description}",
                    paramType="header"
            )
    })*/
    @GetMapping(
            path = "/{id}/addresses",
            produces = {
                    MediaType.APPLICATION_XML_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaTypes.HAL_JSON_VALUE
            }
    )
    public CollectionModel<AddressResource> getUserAddresses(@PathVariable String id) {
        List<AddressResource> addressResourceList = new ArrayList<>();
        List<AddressDto> addressesDTO = addressService.getAddresses(id);
        if (addressesDTO != null && !addressesDTO.isEmpty()) {
            Type listType = new TypeToken<List<AddressResource>>() {}.getType();
            addressResourceList = new ModelMapper().map(addressesDTO, listType);
            for (AddressResource addressResource : addressResourceList) {
                Link addressLink = linkTo(methodOn(UserController.class)
                        .getUserAddress(id, addressResource.getAddressId())
                ).withSelfRel();
                addressResource.add(addressLink);
                Link userLink = linkTo(methodOn(UserController.class).getUser(id)).withRel(USER_LINK_NAME);
                addressResource.add(userLink);
            }
        }
        return CollectionModel.of(addressResourceList);
    }

    /*@ApiImplicitParams({
            @ApiImplicitParam(
                    name="authorization",
                    value="${userController.authorizationHeader.description}",
                    paramType="header"
            )
    })*/
    @GetMapping(
            path = "/{userId}/addresses/{addressId}",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE,
                    MediaTypes.HAL_JSON_VALUE
            }
    )
    public EntityModel<AddressResource> getUserAddress(@PathVariable String userId, @PathVariable String addressId) {
        AddressDto addressesDto = addressService.getAddress(addressId);
        ModelMapper modelMapper = new ModelMapper();
        Link addressLink = linkTo(methodOn(UserController.class).getUserAddress(userId, addressId)).withSelfRel();
        Link userLink = linkTo(UserController.class).slash(userId).withRel(USER_LINK_NAME);
        Link addressesLink = linkTo(methodOn(UserController.class).getUserAddresses(userId)).withRel(ADDRESSES_LINK_NAME);
        AddressResource addressResource = modelMapper.map(addressesDto, AddressResource.class);
        addressResource.add(addressLink);
        addressResource.add(userLink);
        addressResource.add(addressesLink);
        return EntityModel.of(addressResource);
    }

    @GetMapping(
            path = "/email-verification",
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE }
    )
    public OperationStatusModel verifyEmailToken(@RequestParam(value = "token") String token) {
        OperationStatusModel operationStatus = new OperationStatusModel();
        operationStatus.setOperationName(RequestOperationName.VERIFY_EMAIL.name());
        boolean isVerified = userService.verifyEmailToken(token);
        if (isVerified) {
            operationStatus.setOperationResult(RequestOperationStatus.SUCCESS.name());
        } else {
            operationStatus.setOperationResult(RequestOperationStatus.ERROR.name());
        }
        return operationStatus;
    }

    /*
     * http://localhost:8080/mobile-app-ws/users/password-reset-request
     * */
    @PostMapping(
            path = "/password-reset-request",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    public OperationStatusModel requestReset(@RequestBody PasswordResetRequestModel passwordResetRequestModel) {
        OperationStatusModel returnValue = new OperationStatusModel();
        boolean operationResult = userService.requestPasswordReset(passwordResetRequestModel.getEmail());
        returnValue.setOperationName(RequestOperationName.REQUEST_PASSWORD_RESET.name());
        returnValue.setOperationResult(RequestOperationStatus.ERROR.name());
        if (operationResult) {
            returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        }
        return returnValue;
    }

    @PostMapping(
            path = "/password-reset",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    public OperationStatusModel resetPassword(@RequestBody PasswordResetModel passwordResetModel) {
        OperationStatusModel returnValue = new OperationStatusModel();
        boolean operationResult = userService.resetPassword(
                passwordResetModel.getToken(),
                passwordResetModel.getPassword()
        );
        returnValue.setOperationName(RequestOperationName.PASSWORD_RESET.name());
        returnValue.setOperationResult(RequestOperationStatus.ERROR.name());
        if (operationResult) {
            returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        }
        return returnValue;
    }

}

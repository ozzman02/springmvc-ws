package com.appsdeveloperblog.app.ws.bootstrap;

import com.appsdeveloperblog.app.ws.io.entity.AuthorityEntity;
import com.appsdeveloperblog.app.ws.io.entity.RoleEntity;
import com.appsdeveloperblog.app.ws.io.entity.UserEntity;
import com.appsdeveloperblog.app.ws.io.repository.AuthorityRepository;
import com.appsdeveloperblog.app.ws.io.repository.RoleRepository;
import com.appsdeveloperblog.app.ws.io.repository.UserRepository;
import com.appsdeveloperblog.app.ws.shared.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.appsdeveloperblog.app.ws.shared.Authorities.*;
import static com.appsdeveloperblog.app.ws.shared.Roles.ROLE_ADMIN;
import static com.appsdeveloperblog.app.ws.shared.Roles.ROLE_USER;

@Component
public class InitialUsersSetup {

    private final AuthorityRepository authorityRepository;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final Utils utils;

    private final BCryptPasswordEncoder encoder;

    @Autowired
    public InitialUsersSetup(AuthorityRepository authorityRepository,
                             RoleRepository roleRepository,
                             UserRepository userRepository,
                             Utils utils,
                             BCryptPasswordEncoder encoder) {
        this.authorityRepository = authorityRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.utils = utils;
        this.encoder = encoder;
    }

    @EventListener
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        AuthorityEntity readAuthority = createAuthority(READ_AUTHORITY.name());
        AuthorityEntity writeAuthority = createAuthority(WRITE_AUTHORITY.name());
        AuthorityEntity deleteAuthority = createAuthority(DELETE_AUTHORITY.name());

        RoleEntity roleUser = createRole(ROLE_USER.name(), Arrays.asList(readAuthority, writeAuthority));
        RoleEntity roleAdmin = createRole(ROLE_ADMIN.name(),
                Arrays.asList(readAuthority, writeAuthority, deleteAuthority));

        UserEntity adminUser = new UserEntity();
        adminUser.setFirstName("Oscar");
        adminUser.setLastName("Santamaria");
        adminUser.setEmail("osantamaria@gmail.com");
        adminUser.setEmailVerificationStatus(Boolean.TRUE);
        adminUser.setUserId(utils.generateUserId(30));
        adminUser.setEncryptedPassword(encoder.encode("canela01"));
        adminUser.setRoles(List.of(roleAdmin));

        UserEntity user = new UserEntity();
        user.setFirstName("Sergio");
        user.setLastName("Santamaria");
        user.setEmail("sersanta1989@gmail.com");
        user.setEmailVerificationStatus(Boolean.TRUE);
        user.setUserId(utils.generateUserId(30));
        user.setEncryptedPassword(encoder.encode("canela02"));
        user.setRoles(List.of(roleUser));

        userRepository.saveAll(Arrays.asList(adminUser, user));
    }

    @Transactional
    private AuthorityEntity createAuthority(String name) {
        AuthorityEntity authority = authorityRepository.findByName(name);
        if (authority == null) {
            authority = new AuthorityEntity(name);
            authorityRepository.save(authority);
        }
        return authority;
    }

    @Transactional
    private RoleEntity createRole(String name, Collection<AuthorityEntity> authorities) {
        RoleEntity role = roleRepository.findByName(name);
        if (role == null) {
            role = new RoleEntity(name);
            role.setAuthorities(authorities);
            roleRepository.save(role);
        }
        return role;
    }

}

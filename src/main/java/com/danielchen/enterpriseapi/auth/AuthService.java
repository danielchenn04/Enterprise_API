package com.danielchen.enterpriseapi.auth;

import com.danielchen.enterpriseapi.auth.dto.AuthResponse;
import com.danielchen.enterpriseapi.auth.dto.LoginRequest;
import com.danielchen.enterpriseapi.auth.dto.SignupRequest;
import com.danielchen.enterpriseapi.common.ConflictException;
import com.danielchen.enterpriseapi.common.UnauthorizedException;
import com.danielchen.enterpriseapi.security.JwtService;
import com.danielchen.enterpriseapi.tenant.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }

        Organization org = new Organization();
        org.setName(request.orgName());
        organizationRepository.save(org);

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        Membership membership = new Membership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(Role.ADMIN);
        membershipRepository.save(membership);

        String token = jwtService.issue(user.getId(), org.getId(), Role.ADMIN);
        return new AuthResponse(token, user.getId(), org.getId(), Role.ADMIN);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        List<Membership> memberships = membershipRepository.findByUser_Id(user.getId());
        if (memberships.isEmpty()) {
            throw new UnauthorizedException("User has no organization memberships");
        }

        Membership membership;
        if (request.orgId() != null) {
            membership = membershipRepository.findByOrganization_IdAndUser_Id(request.orgId(), user.getId())
                    .orElseThrow(() -> new UnauthorizedException("Not a member of that organization"));
        } else if (memberships.size() == 1) {
            membership = memberships.get(0);
        } else {
            throw new UnauthorizedException(
                    "User belongs to multiple organizations — provide orgId in the request");
        }

        String token = jwtService.issue(
                user.getId(), membership.getOrganization().getId(), membership.getRole());
        return new AuthResponse(
                token, user.getId(), membership.getOrganization().getId(), membership.getRole());
    }
}

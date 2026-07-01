package com.esicsociety.ams.security;

import com.esicsociety.ams.member.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads a member by account number for username/password authentication. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public AppUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String accountNo) throws UsernameNotFoundException {
        return memberRepository.findByAccountNo(accountNo)
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("No account: " + accountNo));
    }
}

package com.sp.fc.user.service;

import com.sp.fc.user.domain.SpOAuth2User;
import com.sp.fc.user.domain.SpUser;
import com.sp.fc.user.domain.SpAuthority;
import com.sp.fc.user.repository.SpOAuth2UserRepository;
import com.sp.fc.user.repository.SpUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpUserService implements UserDetailsService {

    private final SpUserRepository spUserRepository;

    @Autowired
    private SpOAuth2UserRepository oAuth2UserRepository;

    public SpUserService(SpUserRepository spUserRepository) {
        this.spUserRepository = spUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return spUserRepository.findSpUserByEmail(username)
                .orElseThrow(()->new UsernameNotFoundException(username));
    }

    public Optional<SpUser> findUser(String email){
        return spUserRepository.findSpUserByEmail(email);
    }

    public SpUser save(SpUser user){
        return spUserRepository.save(user);
    }

    public void addAuthority(Long userId, String authority){
        spUserRepository.findById(userId).ifPresent(user-> {
            SpAuthority newRole = new SpAuthority(user.getUserId(), authority);
            if(user.getAuthorities() == null){
                HashSet<SpAuthority> authorities = new HashSet<>();
                authorities.add(newRole);
                user.setAuthorities(authorities);
                save(user);
            }else if (!user.getAuthorities().contains(newRole)){
                HashSet<SpAuthority> authorities = new HashSet<>();
                authorities.addAll(user.getAuthorities());
                authorities.add(newRole);
                user.setAuthorities(authorities);
                save(user);
            }
        });
    }

    public void removeAuthority(Long userId, String authority){
        spUserRepository.findById(userId).ifPresent(user->{
            if(user.getAuthorities()==null) return;
            SpAuthority targetRole = new SpAuthority(user.getUserId(), authority);
            if(user.getAuthorities().contains(targetRole)){
                user.setAuthorities(
                        user.getAuthorities().stream().filter(auth->!auth.equals(targetRole)).collect(Collectors.toSet())
                );
                save(user);
            }
        });
    }

    public SpUser load(SpOAuth2User oAuth2User) {
        SpOAuth2User dbUser = oAuth2UserRepository.findById(oAuth2User.getOauth2UserId())
                .orElseGet(()->{
                    SpUser user = new SpUser();
                    user.setEmail(oAuth2User.getEmail());
                    user.setName(oAuth2User.getName());
                    user.setEnabled(true);
                    user = spUserRepository.save(user);
                    addAuthority(user.getUserId(), "ROLE_USER");

                    oAuth2User.setUserId(user.getUserId());
                    return oAuth2UserRepository.save(oAuth2User);
                });
        return spUserRepository.findById(dbUser.getUserId()).get();
    }
}

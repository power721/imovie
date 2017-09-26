package org.har01d.imovie.web.user;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.MovieRepository;
import org.har01d.imovie.web.user.domain.User;
import org.har01d.imovie.web.user.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favourites")
public class FavouriteController {

    private final UserRepository userRepository;

    private final MovieRepository movieRepository;

    public FavouriteController(UserRepository userRepository, MovieRepository movieRepository) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    @GetMapping
    public Page<Favourite> getFavourites(Principal principal, Pageable pageable) {
        User user = userRepository.findByUsername(principal.getName());
        List<Favourite> favourites = user.getFavourite().stream().map(Favourite::new).collect(Collectors.toList());
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int to = from + pageable.getPageSize();
        if (to > favourites.size()) {
            to = favourites.size();
        }
        return new PageImpl<>(favourites.subList(from, to), pageable, favourites.size());
    }

    @PostMapping("{id}")
    public boolean addFavourite(@PathVariable Integer id, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        Movie movie = movieRepository.getOne(id);
        if (user != null && movie != null) {
            user.getFavourite().add(movie);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @DeleteMapping("{id}")
    public boolean deleteFavourite(@PathVariable Integer id, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        Movie movie = movieRepository.getOne(id);
        if (user != null && movie != null) {
            user.getFavourite().remove(movie);
            userRepository.save(user);
        }
        return false;
    }

    @GetMapping("{id}")
    public boolean isFavourite(@PathVariable Integer id, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        Movie movie = movieRepository.getOne(id);
        return user != null && movie != null && user.getFavourite().contains(movie);
    }

}

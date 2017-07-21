package com.example.fluxflixapp;

import com.oracle.webservices.internal.api.databinding.DatabindingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.*;

import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.persistence.*;
import java.awt.*;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class FluxFlixAppApplication {


	public static void main(String[] args) {
		SpringApplication.run(FluxFlixAppApplication.class, args);
	}



	@Bean
	CommandLineRunner demo(MovieRepository movieRepository) {
		return args -> {
			Stream.of("Aeon Flux", "Enter the Mono<Void>", "The fluxinator", "Silence Of Lambdas", "Reactive Mongos on Plane", "Y Tu Mono Tambien", "Attack of fluxxes", "Back to the future")
					.map(name -> new Movie(UUID.randomUUID().toString(), name, randomGenre()))
					.map(movie-> movieRepository.save(movie))
					.forEach(System.out::println);
		};
	}
	private String randomGenre() {
		String[] genres = "horror, romcom, drama, action, documentary".split(",");
		return genres[new Random().nextInt(genres.length)];
	}
}

class MovieEvent {

	private Movie movie;

	public MovieEvent(Movie movie, Date when, String user) {
		this.movie = movie;
		this.when = when;
		this.user = user;
	}
	public MovieEvent() {}

	private Date when;
	private String user;

	public Movie getMovie() {
		return movie;
	}

	public void setMovie(Movie movie) {
		this.movie = movie;
	}

	public Date getWhen() {
		return when;
	}

	public void setWhen(Date when) {
		this.when = when;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

}

@Service
class FluxFlixService {

	private final MovieRepository movieRepository;

	public FluxFlixService(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}
	public Flux<MovieEvent> streamStreams(Movie movie){
		Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
		Flux<MovieEvent> events = Flux.fromStream(Stream.generate(()-> new MovieEvent(movie, new Date(), randomUser())));
		return Flux.zip(interval, events).map(Tuple2::getT2);
	}

	public Flux<Movie> all() {
		return Flux.fromIterable(movieRepository.findAll());
	}

	public Mono<Movie> byId(String id) {
		Optional<Movie> movie = movieRepository.findById(id);
		return Mono.justOrEmpty(movie);
	}
	private String randomUser() {
		String[] users = "alex, phil, anne, serge".split(",");
		return users[new Random().nextInt(users.length)];
	}
}
@RestController
@RequestMapping("/movies")
class MovieRestController {
	private final FluxFlixService fluxFlixService;
	public MovieRestController(FluxFlixService fluxFlixService){
		this.fluxFlixService = fluxFlixService;
	}

	@GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<MovieEvent> events (@PathVariable String id){
		return fluxFlixService.byId(id)
			.flatMapMany(fluxFlixService::streamStreams);
	}
	@GetMapping
	public Flux<Movie> all(){
		return fluxFlixService.all();
	}
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public Mono<Movie> byId(@PathVariable String id) {
		System.out.println("Got the ID: "+id);
		return fluxFlixService.byId(id);
	}
}

@Repository
interface MovieRepository extends CrudRepository<Movie, String> {
}

@Entity
@NoArgsConstructor
@ToString
class Movie {

	@Id
	private String id;
	private String title;

	public Movie(String id, String title, String genre) {
		this.id = id;
		this.title = title;
		this.genre = genre;
	}

	private String genre;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}


	@Override
	public String toString() {
		String s = "Movie: title - "+getTitle()+", genre - "+getGenre();
		return s;
	}
}
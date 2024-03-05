package com.moviesdatabase.moviesdatabaseproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
public class MovieApplication {

  public static void main(String[] args) {
    SpringApplication.run(MovieApplication.class, args);
  }
}

@Document(collection = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Movie {
  @Id
  private String _id;
  private String title;
  private String image;
  private String description;
  private Number rating;
  private Number vote;
  private String genre;
  private Number year;
  private Stats stats; // New property

  // Inner class for stats
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Stats {
    private List<Integer> rating;
    private List<String> review;
  }
}

interface MovieRepository extends MongoRepository<Movie, String> {
  Movie findBy_id(String id);

  List<Movie> findByYearEquals(int year);

  List<Movie> findByTitleContaining(String title);

  List<Movie> findByYearEqualsAndTitleContaining(int year, String title);

  List<Movie> findByGenre(String genre);

  List<Movie> findByYearEqualsAndGenre(int year, String genre);

  List<Movie> findByTitleContainingAndGenre(String title, String genre);

  List<Movie> findByYearEqualsAndTitleContainingAndGenre(int year, String title, String genre);

}

@RestController
@CrossOrigin(origins = "http://localhost:5173")
class MovieController {

  @Autowired
  private MovieRepository movieRepository;

  @GetMapping("/movies")
  public List<Movie> getAllMovies(@RequestParam(value = "year", required = false) Integer year,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "genre", required = false) String genre) {
    if (year != null && title != null && genre != null) {
      // If year, title, and genre parameters are provided, filter movies by all three
      // criteria
      return movieRepository.findByYearEqualsAndTitleContainingAndGenre(year, title, genre);
    } else if (year != null && title != null) {
      // If year and title parameters are provided, filter movies by year and title
      return movieRepository.findByYearEqualsAndTitleContaining(year, title);
    } else if (year != null && genre != null) {
      // If year and genre parameters are provided, filter movies by year and genre
      return movieRepository.findByYearEqualsAndGenre(year, genre);
    } else if (title != null && genre != null) {
      // If title and genre parameters are provided, filter movies by title and genre
      return movieRepository.findByTitleContainingAndGenre(title, genre);
    } else if (year != null) {
      // If only year parameter is provided, filter movies by year
      return movieRepository.findByYearEquals(year);
    } else if (title != null) {
      // If only title parameter is provided, filter movies by title
      return movieRepository.findByTitleContaining(title);
    } else if (genre != null) {
      // If only genre parameter is provided, filter movies by genre
      return movieRepository.findByGenre(genre);
    } else {
      // If no parameters provided, return all movies
      return movieRepository.findAll(Sort.by(Direction.ASC, "title"));
    }
  }

  @GetMapping("/movies/{id}")
  public ResponseEntity<Movie> getMovieById(@PathVariable("id") String id) {
    Movie movie = movieRepository.findBy_id(id);
    if (movie != null) {
      return ResponseEntity.ok(movie);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  // Endpoint to add a new movie
  @PostMapping("/movies")
  public ResponseEntity<Map<String, String>> addMovie(@RequestBody Movie movie) {
    Map<String, String> response = new HashMap<>();
    try {
      movieRepository.save(movie);
      response.put("message", "Movie added successfully");
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      response.put("error", "Internal Server Error");
      response.put("message", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  // Endpoint to update an existing movie
  @PutMapping("/movies/{id}")
  public ResponseEntity<Object> updateMovie(@PathVariable("id") String id, @RequestBody Movie updatedMovie) {
    try {
      Movie existingMovie = movieRepository.findBy_id(id);
      if (existingMovie != null) {
        updatedMovie.set_id(id); // Ensure the ID is set for the updated movie
        movieRepository.save(updatedMovie);
        // Construct the response object with message and updated movie
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Movie updated successfully");
        response.put("updatedMovie", updatedMovie);
        return ResponseEntity.ok(response);
      } else {
        // If movie not found, return not found response
        Map<String, String> response = new HashMap<>();
        response.put("message", "Movie not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      }
    } catch (Exception e) {
      // If any exception occurs, return internal server error response
      Map<String, String> response = new HashMap<>();
      response.put("error", "Internal Server Error");
      response.put("message", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @DeleteMapping("/movies/{id}")
  public ResponseEntity<Map<String, String>> deleteMovie(@PathVariable("id") String id) {
    Map<String, String> response = new HashMap<>();
    try {
      // Check if the movie exists
      if (movieRepository.existsById(id)) {
        // If the movie exists, delete it
        movieRepository.deleteById(id);
        response.put("message", "Movie deleted successfully");
        return ResponseEntity.status(HttpStatus.OK).body(response);
      } else {
        // If the movie doesn't exist, return a not found response
        response.put("error", "Movie not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      }
    } catch (Exception e) {
      // If any exception occurs, return an internal server error response
      response.put("error", "Internal Server Error");
      response.put("message", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  // Endpoint to add review and rating for a movie
  @PostMapping("/movies/{id}/review")
  public ResponseEntity<Object> addReviewAndRating(@PathVariable("id") String id,
      @RequestBody ReviewAndRating reviewAndRating) {
    try {
      // Retrieve the movie by ID
      Movie movie = movieRepository.findBy_id(id);
      if (movie != null) {
        // Get the existing stats or create new stats if it's null
        Movie.Stats stats = movie.getStats();
        if (stats == null) {
          stats = new Movie.Stats();
          stats.setRating(new ArrayList<>());
          stats.setReview(new ArrayList<>());
        }

        // Add review and rating
        stats.getReview().add(reviewAndRating.getReview());
        stats.getRating().add(reviewAndRating.getRating());

        // Update movie with new stats
        movie.setStats(stats);

        // Calculate the average rating
        double averageRating = calculateAverageRating(stats.getRating());
        movie.setRating(averageRating);

        // Update vote to the total number of reviews in stats
        int totalVote = stats.getReview().size();
        movie.setVote(totalVote);

        movieRepository.save(movie);

        // Construct the response object
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Review and rating added successfully");
        response.put("updatedMovie", movie);
        return ResponseEntity.ok(response);
      } else {
        // If movie not found, return not found response
        Map<String, String> response = new HashMap<>();
        response.put("error", "Movie not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      }
    } catch (Exception e) {
      // If any exception occurs, return internal server error response
      Map<String, String> response = new HashMap<>();
      response.put("error", "Internal Server Error");
      response.put("message", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  // Method to calculate the average rating
  private double calculateAverageRating(List<Integer> ratings) {
    if (ratings.isEmpty()) {
      return 0.0;
    }
    int totalRating = 0;
    for (int rating : ratings) {
      totalRating += rating;
    }
    return (double) totalRating / ratings.size();
  }

  // Inner class for review and rating
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReviewAndRating {
    private String review;
    private Integer rating;
  }
}

@Component
class MovieDataRetriever implements CommandLineRunner {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public void run(String... args) throws Exception {
    List<Movie> movies = mongoTemplate.findAll(Movie.class, "movies");
    System.out.println("Movies:");
    for (Movie movie : movies) {
      System.out.println(movie);
    }
  }
}

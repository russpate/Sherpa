package com.fantastipotami.controllers;

import com.fantastipotami.entities.*;
import com.fantastipotami.services.*;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Created by alexanderhughes on 4/6/16.
 */
@RestController
public class SherpaController {

    @Autowired
    CategoryRepository catRepo;
    @Autowired
    LocationCategoryJoinRepository locCatRepo;
    @Autowired
    LocationRepository locRepo;
    @Autowired
    TourLocationJoinRepository tourLocRepo;
    @Autowired
    PermTourLocationJoinRepository pTourLocRepo;
    @Autowired
    TourRepository tourRepo;
    @Autowired
    PermTourRepository pTourRepo;


    Server dbui = null;

    //will read csv files that store our prebuilt tour data
    @PostConstruct
    public void init() throws SQLException, FileNotFoundException {
        dbui = Server.createWebServer().start();
        populateCategoriesTable("categories.tsv");
        populateLocationsTable("locations.tsv");
        populatePermToursTable("permTours.tsv");
    }

    @PreDestroy
    public void destroy() {
        dbui.stop();
    }

    /*Hit to get the perm tour options, they will include each
    * location with all available details, don't need to pass
    * anything*/
    @RequestMapping(path = "/perm-tour", method = RequestMethod.GET)
    public ResponseEntity<Object> getAllTours(HttpSession session) {
//        Integer id = (Integer) session.getAttribute("tourId");
//        if (id != null) {
//            return new ResponseEntity<Object>("A tour is already in progress for this user", HttpStatus.TEMPORARY_REDIRECT);
//        }
        return new ResponseEntity<Object>(pTourRepo.findAll(), HttpStatus.OK);
    }
    /*hit this if user hits continue after being asked if they
    * wished to continue an existing tour (if they opt not to continue
    * then hit the /tour delete route and then hit the post /tour route again
    * to display all the tours for the user to select a knew one
    * just append 0 as the id in the , don't need anything there*/
    @RequestMapping(path = "/tour/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> getCurrentTour(HttpSession session, @PathVariable("id") Integer id) {
        return new ResponseEntity<Object>(tourRepo.findOne(id), HttpStatus.OK);
    }
    //invalidates the session for the tour in progress, i.e. cancel/end
    @RequestMapping(path = "/tour/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> cancelTour(HttpSession session) {
        session.invalidate();
        return new ResponseEntity<Object>(HttpStatus.OK);
    }
    /*hit this to start a tour based on one of the pre-made tours
    * pass the id of the pre-made PermTour as a path variable*/
    @RequestMapping(path = "/tour/{id}", method = RequestMethod.POST)
    public ResponseEntity<Object> joinTour(HttpSession session, @PathVariable("id") int id) {
        PermTour permTour = pTourRepo.findOne(id);
        List<Location> locs = pTourLocRepo.findAllByPermTour(permTour);
        Tour tour = new Tour();
        tour = tourRepo.save(tour);
        for (Location loc : locs) {
            tourLocRepo.save(new TourLocationJoin(loc, tour));
        }
        session.setAttribute("tourId", tour.getId());
        return new ResponseEntity<Object>(tour, HttpStatus.OK);
    }
    //choiceView stuff
    /*use this to get an array all the categories*/
    @RequestMapping(path = "/category", method = RequestMethod.GET)
    public ResponseEntity<Object> getAllCategories() {
        return new ResponseEntity<Object>(catRepo.findAll(), HttpStatus.OK);
    }
    /*give as a path variable the category id from the user selection to get
    * all the locations associated with that category*/
    @RequestMapping(path = "/category/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> getToursByCat(HttpSession session, @PathVariable("id") int id) {
        return new ResponseEntity<Object>(locCatRepo.findAllByCategory(catRepo.findOne(1)), HttpStatus.OK);
    }
    @RequestMapping(path = "/tour", method = RequestMethod.POST)
    public ResponseEntity<Object> buildTour(@RequestBody HashMap<String, List<Integer>> map) {
        List<Integer> locs = map.get("list");
        Tour tour = new Tour();
        tour = tourRepo.save(tour);
        for (int id : locs) {
            tourLocRepo.save(new TourLocationJoin(locRepo.findOne(id), tour));
        }
        return new ResponseEntity<Object>(tour, HttpStatus.OK);
    }

    public void populateCategoriesTable(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        Scanner fileScanner = new Scanner(f);
        fileScanner.nextLine();
        while (fileScanner.hasNext()) {
            String[] columns = fileScanner.nextLine().split("\\t");
            for (String cat : columns) {
                Category category = new Category(cat);
                catRepo.save(category);
            }
        }
    }

    public void populateLocationsTable(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        Scanner fileScanner = new Scanner(f);
        fileScanner.nextLine();
        while (fileScanner.hasNext()) {
            String[] columns = fileScanner.nextLine().split("\\t");
            Location location = new Location(columns[3], columns[4], Double.valueOf(columns[5]), Double.valueOf(columns[6]));
            if (!columns[0].isEmpty()) {
                location.setImageUrl(columns[0]);
            }
            if (!columns[1].isEmpty()) {
                location.setSiteUrl(columns[1]);
            }
            if (!columns[2].isEmpty()) {
                location.setDescription(columns[2]);
            }
            location = locRepo.save(location);
            String[] cats = columns[7].split(",");
            for (String cat : cats) {
                 LocationCategoryJoin lcj = new LocationCategoryJoin(location, catRepo.findByCategoryStr(cat));
                locCatRepo.save(lcj);
            }
        }
    }

    public void populatePermToursTable(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        Scanner fileScanner = new Scanner(f);
        fileScanner.nextLine();
        for (int i = 0; i < 4; i++) {
            PermTour permTour = new PermTour();
            permTour = pTourRepo.save(permTour);
        }
        while (fileScanner.hasNext()) {
            String[] columns = fileScanner.nextLine().split("\\t");
            PermTourLocationJoin tlj = new PermTourLocationJoin(locRepo.findOne(Integer.valueOf(columns[1])), pTourRepo.findOne(Integer.valueOf(columns[0])));
            pTourLocRepo.save(tlj);

        }
    }
}

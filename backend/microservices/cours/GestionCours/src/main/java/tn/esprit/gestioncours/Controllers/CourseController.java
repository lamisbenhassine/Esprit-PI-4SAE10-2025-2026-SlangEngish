package tn.esprit.gestioncours.Controllers;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gestioncours.Entities.Course;
import tn.esprit.gestioncours.Services.ICourseService;

import java.util.List;

@RestController
@RequestMapping("/course")
public class CourseController {

    private final ICourseService courseService;

    public CourseController(@Qualifier("courseServiceImpl") ICourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/add")
    public Course addCourse(@RequestBody Course course) {
        return courseService.addCourse(course);
    }

    @PutMapping("/update/{id}")
    public Course updateCourse(@PathVariable Long id, @RequestBody Course course) {
        Course existingCourse = courseService.retrieveCourse(id);
        if (existingCourse != null) {
            existingCourse.setName(course.getName());
            existingCourse.setLevel(course.getLevel());
            existingCourse.setDescription(course.getDescription());
            existingCourse.setImageUrl(course.getImageUrl());
            return courseService.updateCourse(existingCourse);
        }
        return null;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @GetMapping("/get/{id}")
    public Course getCourse(@PathVariable Long id) {
        return courseService.retrieveCourse(id);
    }

    @GetMapping("/all")
    public List<Course> getAllCourses() {
        return courseService.retrieveAllCourses();
    }
}
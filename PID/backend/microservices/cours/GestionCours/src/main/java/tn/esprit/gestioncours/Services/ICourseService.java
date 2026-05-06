package tn.esprit.gestioncours.Services;

import tn.esprit.gestioncours.Entities.Course;

import java.util.List;

public interface ICourseService {

    Course addCourse(Course course);

    Course updateCourse(Course course);

    void deleteCourse(Long id);

    Course retrieveCourse(Long id);

    List<Course> retrieveAllCourses();
}
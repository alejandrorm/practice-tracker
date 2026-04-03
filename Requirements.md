# PracticeTracker

## General idea
This application helps a student log and keep of track of their daily practice
 goals for an instrument, for example violin.
 
## Requirements

### UI 

Easy to use, not overloaded with graphics, but also unique look, not just android stock

### Functionality

The app should have 3 mains modes:

* Practice time
* Summary and stats time
* Organization time

#### Practice time

This mode allows the user track what they are practicing in a given session, it
should list:

* Pieces the student can/should practice in this session, whether they are scales, etudes,
 songs, concertos, etc
* For each piece, it can optionally show more information about it, such as the name of 
the composition, author, book and pages where to find it, etc
* For each piece, there should also be a check list of things the student should/could focus on
why practicing, e.g. "hand position, entonation, rythm, pausing before shifting, bow position, 
meassures 10-25" etc. This should be a customizable check list that students can mark as done
* For each piece, there should be a suggeste pratice time (in minutes) and the student can 
indicate when practice started and when practice ended, the app
should automatically log for how long the student practiced, which skills they focused on 
(from the checklist) and when, and then automatically move to the next piece in the practice plan
* The student can skip any piece in the day plan

#### Summary and stats time

This should display a dashboard where students can check their average daily practice time,
number of times they have practiced in the last 7/30/365 days. They also should be able to
drill down by piece and by skill.

#### Organization time

This should allow the students to create/clone/edit/delete their practice plans, that is, 
the lists of pieces and the skill checklist for each piece. Students should be able to assign
practice plans to days of the week (everyday, every other day, every Tuesday, etc...)
The app should also offer suggestions when creating these plans, such as suggesting scales or
 skills based on the pieces in the plan.
 
### Other requirements

* The app should allow the user to create a user profile associated with all the data
* The data should be stored locally in the device
* The app should offer the user visual rewards, badges, congratulations for practice streaks,
personal records on amount of time practice 
* Optionally send the user notifications when it's time to practice (practice schedule can be set 
by the user of infer based on the recorded practice habits)
* Optionally offer to share badges (via common share methods on a mobile) when the user reaches
a milestone (practice streal, total number of hours of practice, number of hours of practice 
in a given piece)

### Technical requirements

This should be a mobile app for android, and should work on android phones released in the last
5 years

### Other things of note

The main idea is to use this app as a practice plan/log for violin students, and it should
match that aestethics and that functionality whenever it make sense, but otherwise it should
generalize to other instruments and even other disciplines that would be practiced in the same
style of breaking practice time into a list of daily excercises
  


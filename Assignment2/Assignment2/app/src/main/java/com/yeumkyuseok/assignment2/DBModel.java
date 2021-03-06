package com.yeumkyuseok.assignment2;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.yeumkyuseok.pracgrader.DBSchema.*;

public class DBModel {
    SQLiteDatabase db;

    public void addUser(User user){
        ContentValues cv = new ContentValues();
        cv.put(UserTable.Cols.NAME, user.getName());
        cv.put(UserTable.Cols.EMAIL, user.getEmail());
        cv.put(UserTable.Cols.USERNAME, user.getUser_name());
        cv.put(UserTable.Cols.PASSWORD, user.getPassword());
        cv.put(UserTable.Cols.COUNTRY, user.getCountry());
        cv.put(UserTable.Cols.ROLE, user.getRole());
        cv.put(UserTable.Cols.ADDEDBY, user.getAdded_by());

        db.insert(UserTable.NAME, null, cv);
    }

    public void editUser(String username, User user) {
        ContentValues cv = new ContentValues();

        cv.put(UserTable.Cols.NAME, user.getName());
        cv.put(UserTable.Cols.EMAIL, user.getEmail());
        cv.put(UserTable.Cols.USERNAME, username);
        cv.put(UserTable.Cols.PASSWORD, user.getPassword());
        cv.put(UserTable.Cols.COUNTRY, user.getCountry());
        cv.put(UserTable.Cols.ROLE, user.getRole());
        cv.put(UserTable.Cols.ADDEDBY, user.getAdded_by());

        String[] whereValue =  {username};

        db.update(UserTable.NAME, cv, UserTable.Cols.USERNAME + " = ?" , whereValue);
    }

    public void deleteUser(User user){
        String[] whereValue= {String.valueOf(user.getUser_name())};
        db.delete(UserTable.NAME, UserTable.Cols.USERNAME + " = ?", whereValue );
    }

    public void addPractical(Practical practical) {
        ContentValues cv = new ContentValues();
        cv.put(PracticalTable.Cols.TITLE, practical.getTitle());
        cv.put(PracticalTable.Cols.DESC, practical.getDescription());
        cv.put(PracticalTable.Cols.MARK, practical.getMark());
        db.insert(PracticalTable.NAME, null, cv);
    }

    public void editPractical(String title, Practical practical) {
        ContentValues cv = new ContentValues();


        cv.put(PracticalTable.Cols.TITLE, practical.getTitle());
        cv.put(PracticalTable.Cols.DESC, practical.getDescription());
        cv.put(PracticalTable.Cols.MARK, practical.getMark());

        String[] whereValue =  {title};
        db.update(PracticalTable.NAME, cv, PracticalTable.Cols.TITLE + " = ?" , whereValue);
    }


    public void deletePractical(Practical practical) {
        String[] whereValue= {String.valueOf(practical.getTitle())};
        db.delete(PracticalTable.NAME, PracticalTable.Cols.TITLE + " = ?", whereValue );
    }

    public void addTakenPrac(TakenPrac takenPrac) {
        ContentValues cv = new ContentValues();
        cv.put(TakenPracTable.Cols.USERNAME, takenPrac.getUsername());
        cv.put(TakenPracTable.Cols.PRAC_TITLE, takenPrac.getPracTitle());
        cv.put(TakenPracTable.Cols.MARK_SCORED, takenPrac.getMarkScored());
        db.insert(TakenPracTable.NAME, null, cv);
    }

    public void editTakenPrac(String username, String pracTitle, double markScored) {
        ContentValues cv = new ContentValues();

        cv.put(TakenPracTable.Cols.USERNAME, username);
        cv.put(TakenPracTable.Cols.PRAC_TITLE, pracTitle);
        cv.put(TakenPracTable.Cols.MARK_SCORED, markScored);

        String[] whereValue =  {username, pracTitle};

        db.update(TakenPracTable.NAME, cv, TakenPracTable.Cols.USERNAME + " = ? AND " + TakenPracTable.Cols.PRAC_TITLE + " = ?" , whereValue);
    }

    public void deleteTakenPrac(String username, String pracTitle) {
        String[] whereValue= {username, pracTitle};
        db.delete(TakenPracTable.NAME, TakenPracTable.Cols.USERNAME + " = ? AND " + TakenPracTable.Cols.PRAC_TITLE + " = ?", whereValue );
    }

}

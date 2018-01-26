package io.left.meshenger.Database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import io.left.meshenger.Models.MeshIDTuple;
import io.left.meshenger.Models.User;
import io.left.rightmesh.id.MeshID;

/**
 * A collection of queries for accessing data types for MeshIM.
 */
@Dao
public interface MeshIMDao {
    @Insert()
    public void insertUsers(User... users);

    @Update
    public void updateUsers(User... users);

    @Query("SELECT * FROM Users")
    public User[] fetchAllUsers();

    @Query("SELECT UserId, UserMeshID FROM Users WHERE UserMeshID = :meshId")
    public MeshIDTuple fetchMeshIdTupleByMeshId(MeshID meshId);
}
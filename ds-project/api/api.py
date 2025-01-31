from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import List
import psycopg2
from psycopg2.extras import RealDictCursor
from os import getenv

app = FastAPI()

# Serve static files (frontend)
app.mount("/static", StaticFiles(directory="static"), name="static")


# Database connection
def get_db_connection():
    """
    Establish a connection to the PostgreSQL database using environment variables for configuration.

    Returns:
        conn: A connection object to the PostgreSQL database.
    """
    conn = psycopg2.connect(
        dbname=getenv("DB_NAME", "mydb"),
        user=getenv("DB_USER", "user"),
        password=getenv("DB_PASSWORD", "password"),
        host=getenv("DB_HOST", "localhost"),
        port=getenv("DB_PORT", "5432"),
        cursor_factory=RealDictCursor,
    )
    return conn


class AssignmentResult(BaseModel):
    assignment: int
    passed: bool
    uid: int


class User(BaseModel):
    uid: int
    name: str


@app.get("/")
def read_root():
    """
    Serve the frontend page.

    Returns:
        FileResponse: The index.html file from the static directory.
    """
    return FileResponse("static/index.html")


@app.get("/users", response_model=List[User])
def get_users():
    """
    Fetch all users from the database.

    Returns:
        List[User]: A list of users.

    Raises:
        HTTPException: If no users are found in the database.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT uid, name FROM users")
            users = cursor.fetchall()
            if users:
                return users
            raise HTTPException(status_code=404, detail="No users found")


@app.get("/users/{user_id}", response_model=User)
def get_user(user_id: int):
    """
    Fetch a single user by ID.

    Args:
        user_id (int): The ID of the user to fetch.

    Returns:
        User: The user with the specified ID.

    Raises:
        HTTPException: If the user is not found in the database.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT uid, name FROM users WHERE uid = %s", (user_id,))
            user = cursor.fetchone()
            if user:
                return user
            raise HTTPException(status_code=404, detail="User not found")


@app.get("/users/{user_id}/assignments", response_model=List[AssignmentResult])
def get_user_assignments(user_id: int):
    """
    Fetch all assignments for a given user.

    Args:
        user_id (int): The ID of the user whose assignments to fetch.

    Returns:
        List[AssignmentResult]: A list of assignments for the specified user.

    Raises:
        HTTPException: If no assignments are found for the user.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT assignment, passed, uid FROM assignment_results WHERE uid = %s",
                (user_id,),
            )
            assignments = cursor.fetchall()
            if assignments:
                return assignments
            raise HTTPException(
                status_code=404, detail=f"User with id {user_id} not found"
            )


@app.get("/assignments/{assignment_number}", response_model=List[AssignmentResult])
def get_assignments(assignment_number: int):
    """
    Fetch all users who took a specific assignment.

    Args:
        assignment_number (int): The assignment number to fetch results for.

    Returns:
        List[AssignmentResult]: A list of assignment results for the specified assignment number.

    Raises:
        HTTPException: If no results are found for the specified assignment number.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT assignment, passed, uid FROM assignment_results WHERE assignment = %s",
                (assignment_number,),
            )
            assignments = cursor.fetchall()
            if assignments:
                return assignments
            raise HTTPException(
                status_code=404, detail=f"Assignment-{assignment_number} not found"
            )


@app.get("/assignments", response_model=List[AssignmentResult])
def get_all_assignments():
    """
    Fetch all assignments from the database.

    Returns:
        List[AssignmentResult]: A list of all assignment results.

    Raises:
        HTTPException: If no assignments are found in the database.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT assignment, passed, uid FROM assignment_results")
            assignments = cursor.fetchall()
            if assignments:
                return assignments
            raise HTTPException(status_code=404, detail="No assignments found")


@app.get("/online")
def get_currently_online():
    """
    Fetch all users who are currently online.

    Returns:
        List[dict]: A list of users who are currently online.

    Raises:
        HTTPException: If no users are currently online.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT uid, ip FROM users_online")
            users = cursor.fetchall()
            if users:
                return users
            raise HTTPException(status_code=404, detail="No users are online")

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
    """Serve the frontend page."""
    return FileResponse("static/index.html")


@app.get("/users", response_model=List[User])
def get_users():
    """Fetch all users from the database."""
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT uid, name FROM users")
            users = cursor.fetchall()
            if users:
                return users
            raise HTTPException(status_code=404, detail="No users found")


@app.get("/users/{user_id}", response_model=User)
def get_user(user_id: int):
    """Fetch a single user by ID."""
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT uid, name FROM users WHERE uid = %s", (user_id,))
            user = cursor.fetchone()
            if user:
                return user
            raise HTTPException(status_code=404, detail="User not found")


@app.get("/users/{user_id}/assignments", response_model=List[AssignmentResult])
def get_user_assignments(user_id: int):
    """Fetch all assignments for a given user."""
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
    """Fetch all users who took a specific assignment."""
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
    """Fetch all assignments from the database."""
    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT assignment, passed, uid FROM assignment_results")
            assignments = cursor.fetchall()
            if assignments:
                return assignments
            raise HTTPException(status_code=404, detail="No assignments found")

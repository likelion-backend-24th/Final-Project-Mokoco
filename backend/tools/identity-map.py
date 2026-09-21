"""Generate staging SQL from a UTF-8 CSV exported from user-service: user_id,email.
Usage: python backend/tools/identity-map.py users.csv > identity-map.sql
This writes SQL only; it never connects to a database.
"""
import csv
import sys


def generate(path):
    rows = []
    emails = set()
    with open(path, encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            user_id = int(row["user_id"])
            email = row["email"]
            if user_id <= 0 or not email or len(email) > 255 or email in emails:
                raise ValueError("Invalid or duplicate identity mapping")
            emails.add(email)
            rows.append((user_id, email.encode("utf-8").hex()))
    print("CREATE TABLE IF NOT EXISTS identity_migration_users (email VARCHAR(255) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY, user_id BIGINT NOT NULL, CONSTRAINT ck_identity_positive CHECK(user_id > 0)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;")
    print("START TRANSACTION;")
    for user_id, encoded in rows:
        print(f"INSERT INTO identity_migration_users(email,user_id) VALUES (CONVERT(X'{encoded}' USING utf8mb4),{user_id});")
    print("COMMIT;")


if __name__ == "__main__":
    generate(sys.argv[1])

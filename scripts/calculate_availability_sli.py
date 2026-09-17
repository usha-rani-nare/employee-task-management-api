"""
Small script to calculate an availability SLI from request logs and compare
it against the SLO target. Not fancy - just reads a CSV of request outcomes
and does the math, but it's enough to actually talk about in an interview
instead of just saying "I know what SLI/SLO means."

Expected CSV format (request_log.csv):
    timestamp,status_code
    2026-09-10T10:00:01,200
    2026-09-10T10:00:02,200
    2026-09-10T10:00:03,500
    ...
"""

import csv
import sys

SLO_TARGET = 99.0  # % - matches the SLO defined in README.md


def calculate_availability(log_path: str) -> None:
    total = 0
    successful = 0

    with open(log_path, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            total += 1
            status_code = int(row["status_code"])
            if 200 <= status_code < 400:
                successful += 1

    if total == 0:
        print("No requests found in log file.")
        return

    availability = (successful / total) * 100

    print(f"Total requests:      {total}")
    print(f"Successful requests: {successful}")
    print(f"Failed requests:     {total - successful}")
    print(f"Availability (SLI):  {availability:.2f}%")
    print(f"SLO target:          {SLO_TARGET}%")

    if availability >= SLO_TARGET:
        print("Status: within SLO")
    else:
        print("Status: SLO BREACHED - investigate failed requests")


if __name__ == "__main__":
    log_file = sys.argv[1] if len(sys.argv) > 1 else "request_log.csv"
    calculate_availability(log_file)

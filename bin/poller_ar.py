#!/usr/bin/env python

import sys
from ConfigParser import SafeConfigParser

from argparse import ArgumentParser
from pymongo import MongoClient
from bson.objectid import ObjectId
import subprocess
from argolog import init_log


def get_poller_config(fn_ar_cfg="/etc/ar-compute-engine.conf", logging_config='logging', default_config='default'):
    """
    Initialize the logger and retrieve the settings for the poller
    :param fn_ar_cfg: file from which to retrieve configuration
    :param logging_config: logging section of the configuration
    :param default_config: default section of the configuration
    :return: logger instance, mongo hostname, mongo port and threshold of running recomputations in a tuple
    """
    # Read Configuration file
    ArConfig = SafeConfigParser()
    ArConfig.read(fn_ar_cfg)

    # Initialize logging
    log_mode = ArConfig.get(logging_config, 'log_mode')
    log_file = ArConfig.get(logging_config, 'log_file') if log_mode == 'file' else None
    log_level = ArConfig.get(logging_config, 'log_level')
    log = init_log(log_mode, log_file, log_level, 'argo.poller')

    # Get mongo configurations
    mongo_host = ArConfig.get(default_config, 'mongo_host')
    mongo_port = ArConfig.get(default_config, 'mongo_port')

    threshold = int(ArConfig.get(default_config, 'recomp_threshold'))
    log.info("Threshold: %s", threshold)
    return log, mongo_host, mongo_port, threshold


def get_mongo_collection(mongo_host, mongo_port):
    """
    :return: pymongo collection object of the recalculations collection
    """
    client = MongoClient(mongo_host, int(mongo_port))
    db = client["AR"]
    col = db["recalculations"]
    return col


def get_pending_and_running(col):
    """
    :param col: pymongo collection object
    :return: number of pending requests and running recalculation requests
    """
    num_pen = col.find({"s": "pending"}).count()
    num_run = col.find({"s": "running"}).count()
    return num_pen, num_run


def run_recomputation(col,tenant,num_running,threshold):
    """
    Retrives the first pending recalculation in the db request and queues it for recalculation
    :param col: pymongo collection object
    :param tenant: tenant name
    :param num_running: number of running processes
    :param threshold: threshold number
    """

    # Threshold checks
    if num_running == 0:
        raise ValueError("Zero pending recomputations")
    elif num_running >= threshold:
        raise ValueError("Over threshold; no recomputation will be executed.")  

    pen_recalc = col.find_one({"s": "pending"})
    pen_recalc_id = str(pen_recalc["_id"])

    # Status update allready implemented in recompute
    # Call recompute execution script
    cmd_exec=["./recompute.py","-i",pen_recalc_id,"-t",tenant]
    # Kickstart executor and continue own execution
    subprocess.Popen(cmd_exec)


def main(args=None):
    """
    Checks if there are any pending recomputation requests and if the running
    requests do not exceed a threshold and queues another one to be recomputed
    :param args:
    :return:
    """
    log, mongo_host, mongo_port, threshold = get_poller_config()
    col = get_mongo_collection(mongo_host=mongo_host, mongo_port=mongo_port)
    num_pen, num_run = get_pending_and_running(col)
    log.info("Running recalculations: %s (threshold: %s)", num_run, threshold)
    try:
        run_recomputation(col,args.tenant,num_run,threshold)
        log.info("Below threshold recomputation sent for execution")
    except ValueError as ex:
        log.info(ex)
    


if __name__ == "__main__":
    # Feed Argument parser with the description
    # No arguments needed
    arg_parser = ArgumentParser(
        description="Polling for pending recomputations requests")
    arg_parser.add_argument(
        "-t", "--tenant", help="tenant owner", dest="tenant", metavar="STRING", required="TRUE")
    # Parse the command line arguments accordingly and introduce them to
    # main...
    sys.exit(main(arg_parser.parse_args()))
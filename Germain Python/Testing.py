#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jun 15 14:40:13 2017

@author: alfordsimon
"""

import csv
import dateutil.parser
import matplotlib.pyplot as plt
import matplotlib.dates as dates

f = open('First_Small.csv', 'r')
reader = csv.reader(f)
rows = [row for row in reader][1:]
times = [dateutil.parser.parse(row[0].replace('.', ':', 2)) for row in rows]
users = [row[2] for row in rows]
unique_users = []
num_times = [dates.date2num(t) for t in times]
user_dict = {}
for num_time, user in zip(num_times, users):
	if user not in user_dict:
		user_dict[user] = [num_time]
		unique_users.append(user)
	else:
		user_dict[user].append(num_time)

plt.plot(user_dict[unique_users[0]])
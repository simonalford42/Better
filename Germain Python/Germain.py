#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import csv
import dateutil.parser
import matplotlib.pyplot as plt
import matplotlib.dates as dates
import numpy as np

f = open('First_Small.csv', 'r', encoding = 'latin1')
reader = csv.reader(f)
rows = [row for row in reader][1:]
times = [row[0].replace('.', ':', 2) for row in rows]
times = [t[0:21] + t[27:] for t in times]
times = [dateutil.parser.parse(t) for t in times]
users = [row[2] for row in rows]
unique_users = []
num_times = [dates.date2num(t) for t in times]
user_dict = {}
d2 = {}
for num_time, time, user in zip(num_times, times, users):
    if user not in user_dict:
        user_dict[user] = [num_time]
        unique_users.append(user)
        d2[user] = [time]
    else:
        user_dict[user].append(num_time)
        d2[user].append([time])

for user in user_dict:
    user_dict[user].sort()
        
#for user in user_dict:
#    plt.plot(user_dict[user])
    
dt_list = []    
for user in user_dict:
    times = user_dict[user]
    dt_list.extend([times[i+1] - times[i] for i in range(0, len(times) - 1)])

a = plt.hist(dt_list, bins = 50)
plt.show()
print(a)


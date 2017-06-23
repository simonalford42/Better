#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Jun 21 15:43:56 2017

@author: alfordsimon
"""
from matplotlib import pyplot as plt
import numpy as np

def plot(raw_data):
    # Create a figure with a single subplot
    f, ax = plt.subplots(1, figsize=(10,5))
    
    # Set bar width at 1
    bar_width = 1
    
    # positions of the left bar-boundaries
    bar_l = [i for i in range(len(raw_data[0]))]
    
    # positions of the x-axis ticks (center of the bars as bar labels)
    tick_pos = [i+(bar_width/2) for i in bar_l]
    
    # Create the total score for each participant
    totals = raw_data.sum(axis=0)
    
    # Create the percentage of the total score the pre_score value for each participant was
    percents = 100*raw_data / totals
    
    bottom = [0]*raw_data.shape[1]
    color_list = plt.cm.Set1(np.linspace(0, 1, raw_data.shape[0]))
    for i in range(raw_data.shape[0]):
    # Create a bar chart in position bar_1
        ax.bar(bar_l,
               # using pre_rel data
               percents[i],
               bottom = bottom,
               # with alpha
               alpha=0.9,
               # with color
               color=color_list[i],
               # with bar width
               width=bar_width,
               # with border color
               edgecolor='green'
               )
        bottom = [bottom[j] + percents[i][j] for j in range(raw_data.shape[1])]
    plt.ylim(0, 100)
    plt.xlim(0, raw_data.shape[1])
    
    # shot plot
    plt.show()
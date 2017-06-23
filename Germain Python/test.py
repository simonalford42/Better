#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Jun 21 09:58:32 2017

@author: alfordsimon
"""
import numpy as np
from matplotlib import pyplot as plt
import sys
import percentageBar
import pandas as pd
import mk
import mk_cat
import utilities
import utils_cat
import math
import re
import random

def hist():
    data = open('seq_lengths.csv')
    bins = np.arange(0, 60, 1)
    plt.xlim([-1, 61])
    plt.hist(data, bins=bins, alpha=0.5)
    plt.show()

def pause():
    s = input('')
    if s == 'N':
        sys.exit()

def frequency2():
    for i in [3, 5, 10, 23, 62]:
        with open(str(i) + "pt sequences.txt", 'r') as f:
            seqs = f.readlines()
            seqs = [s.strip() for s in seqs]
            max_l = max(len(s) for s in seqs)
            num_this_long = [sum([int(len(s)/5) == x for s in seqs]) for x in range(int(max_l / 5))]
            
            max_l = sum(num_this_long[i] > 1 for i in range(len(num_this_long)))
            labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz"[:i]
            values = [[0.0]*max_l for x in range(len(labels))]
            freqs = dict(zip(labels, values))
            
            # find average makeup vs different lengths
            for s in seqs:
                b = int(len(s) / 5)
                if b >= max_l:
                    continue
                for j in s:
                    freqs[j][b] += 1.0
            
            freqs = np.array(list(freqs.values()))
            percentageBar.plot(freqs)
    
    
def freqGraph():
    for i in [3, 5, 10, 23, 62]:
        with open(str(i) + "pt sequences.txt", 'r') as f:
            seqs = f.readlines()
            seqs = [s.strip() for s in seqs]
            max_l = max(len(s) for s in seqs)
            num_this_long = [sum([len(s) > x for s in seqs]) for x in range(max_l)]
            
            max_l = sum(num_this_long[i] > 10 for i in range(len(num_this_long)))
            labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz"[:i]
            freqs = []
            for k in range(len(labels)):
                l = [0.0]*max_l
                freqs.append(l)
            for k in range(len(labels)):
                for s in seqs:
                    for j in range(min(len(s), max_l)):
                        if labels[k] == s[j]:
                            freqs[k][j] += 1.0
            
            for k in range(len(labels)):
                for j in range(max_l):
                    freqs[k][j] /= num_this_long[j]
            
            freqs = np.array(freqs)
            percentageBar.plot(freqs)
        
def make_time_series():
    
    vol = .30
    lag = 300
    df = pd.DataFrame(np.random.randn(200000) * np.sqrt(vol) * np.sqrt(1 / 252.)).cumsum()
    return pd.rolling_mean(df, lag).values.tolist()
    
def test_numerical():
    timeseries = np.array(make_time_series())
    timeseries = [t for t in timeseries if not math.isnan(t)]
    #plt.plot(timeseries)
    l = 200
    obj = mk.mk_eab(l=l, metric='euclidean', r=10)
    motif_a, motif_b = obj.search(timeseries)
    d = [0]*(len(timeseries)-l)
    
    
    for i in range(len(timeseries) - l):
        window = timeseries[i:i+l]
        d[i] = utilities.funcs.euclidean(motif_a['motif'], window)[0]
    
    max_dist1 = max(sorted(d)[:l*4])
    
    i = 0
    k = []
    j = []
    
    while i < len(timeseries) - l:
        if d[i] < max_dist1:
            j.append(d[i])
            for m in range(l):
                k.append(timeseries[i])
                i+=1
        else:
            i+=1
            
    i = 0
    print(j)
    fig, ax = plt.subplots()
    plt.show()
    ax.plot(k)
    ax.grid(True)
    ticklines = ax.get_xticklines() + ax.get_yticklines()
    gridlines = ax.get_xgridlines() + ax.get_ygridlines()
    plt.xticks(np.arange(0, l*15, l))
    for line in ticklines:
        line.set_linewidth(3)
    
    for line in gridlines:
        line.set_linestyle('-.')

def test_cat():
    s = str(open('Harry Potter.txt', 'r', encoding = 'utf8').readlines())
    s = re.sub(r'\W+', '', s)
    alph = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890'
    l = [alph.index(char) for char in s]
    t = ''.join([alph[i] for i in l])
    print(t==s) # just to show this string converting is sound
    
    # what sequence can be perfectly found with 0 distance
    key = 'HarryPotterandthePhilosophersStoneJKRowlingnnn'
    # translated to numeric form
    num_key = [alph.index(char) for char in key]
    occ = [i for i in range(len(l)) if l[i:i+len(num_key)] == num_key]
    hit1 = l[occ[0] : occ[0] + len(num_key)]
    hit2 = l[occ[1] : occ[1] + len(num_key)]
    print(hit1, hit2)
    print(utilities.funcs.euclidean(hit1, hit2))
    
    obj = mk.mk_eab(l=len(key), metric='euclidean', r=10)
    motif_a, motif_b = obj.search(l)
    best1 = motif_a['motif']
    best2 = motif_b['motif']
    print(best1)
    print(best2)
    print(utils_cat.funcs.euclidean(best1, best2))
    
test_cat()
    
    
    
    
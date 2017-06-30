#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jun 30 12:18:53 2017

@author: alfordsimon
"""
import pandas as pd
import numpy as np
import Helper
from datetime import datetime

def import_raw(*file_paths):
    datas = []
    
    for file_path in file_paths:
        df = pd.io.parsers.read_csv(file_path, quotechar = '"', header=0, dtype = str, encoding = 'ISO-8859-1', parse_dates = [0])
        datas.append(df.values)
    
    if len(file_paths) == 1:
        return datas[0]
    else:
        return datas
def date_parser(str_format):
    def parse(date):
        return datetime.strptime(date, str_format)
    return parse
    
def sort(datas):
    sorted_data = datas[0]
    for data in datas[1:]:

def extract_seqs(data):
    seqs = []
    for i in range(len(data)):
        
        

def categorize(data):
    new_data = []
    key = []
    for col in data.T:
        id_dict = {}
        next_id = 0
        new = [0]*len(col)
        
        for i, item in enumerate(col):
            if item not in id_dict:
                id_dict[item] = next_id
                next_id += 1
            new[i] = id_dict[item]
         
        new_data.append(new)
        key.append(id_dict)
        
    return np.array(new_data), key
            
raw_data = import_raw('/Users/alfordsimon/Desktop/Germain data/June 28/simon-20170619.1.csv')
data, key = categorize(raw_data)
print(data[0:10])
for id_dict in key:
    print(id_dict)
    Helper.pause()
    
datetime.strptime(string, format)
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jun 27 16:53:48 2017

@author: alfordsimon
"""
import numpy as np
import random
import sys

def pause():
    s = input('')
    if s == 'N':
        sys.exit()

        
def gen_data():
    file = open('toy_hmm.txt', 'w')
    sep = 'AD'
    probs = [[(0.75, 'B'), (0.25, 'A')], [(0.5, 'B'), (0.5, 'C')]]
    
    for i in range(100):
        s = ''
        for j in range(random.randint(4, 10)):
            for state_probs in probs:
                s += sep
                for i in range(4):
                    if random.random() < state_probs[0][0]:
                        s += state_probs[0][1]
                    else:
                        s += state_probs[1][1]
        print(s)
        file.write(s + '\n')
        
def load_data(file_path):
    file = open(file_path,'r')
    lines = file.readlines()
    lines = [l[0:-2] for l in lines]
    lines = [list(l) for l in lines]
    return lines
    
def gen_seqs(pi, A, B, states, num_seqs = 10, seq_length = 25):
    seqs = []
    h_states = list(range(len(pi)))
    for _ in range(num_seqs):
        seq = []
        hs = np.random.choice(h_states, p = pi)
        for _2 in range(seq_length):
            e = np.random.choice(states, p = B[hs])
            seq.append(e)
            hs = np.random.choice(h_states, p = A[hs])
        seqs.append(seq)

    return seqs
    """
    Not even really that much faster, just slightly convienient and doesn't 
    recalculate log_A, log_B, log_pi, while we're at it
    """
def viterbi(pi, A, B, states, id_dict, data):
    N = len(pi)
    log_A = np.log(A)
    log_B = np.log(B)
    log_pi = np.log(pi)  
    Xs = []
    
    for seq in data:
        num_seq = [id_dict[s] for s in seq]
        T = len(seq)
        V = np.empty((T, N))
        pointers = np.zeros((T, N), dtype = np.int32)
    
        V[0] = log_pi + log_B[:, num_seq[0]]
        for t in range(1, T):
            probs = log_A + log_B[:, num_seq[t]] + V[t-1][:,np.newaxis]
            # indices[n] gives h-state that went to n'th state with
            # most probability at time t
            indices = np.argmax(probs, axis=0)
            pointers[t] = indices
            V[t] = probs[indices, range(N)]
        
        X = np.zeros(T)
        index = np.argmax(V[T-1])
        X[T-1] = index
    
        for t in range(T-2, -1, -1):
            prev_index = pointers[t+1][index]
            X[t] = prev_index
            index = prev_index
            
        Xs.append(X)

    return Xs
    
def single_viterbi(pi, A, B, states, id_dict, seq):
    num_seq = [id_dict[s] for s in seq]
    T = len(seq)
    N = len(pi)
    V = np.empty((T, N))
    pointers = np.zeros((T, N), dtype = np.int32)
    
    log_A = np.log(A)
    log_B = np.log(B)
    log_pi = np.log(pi)
    
    V[0] = log_pi + log_B[:,num_seq[0]]

    for t in range(1, T):
        probs = log_A + log_B[:, num_seq[t]] + V[t-1][:,np.newaxis]
        # indices[n] gives h-state that went to n'th state with
        # most probability at time t
        indices = np.argmax(probs, axis=0)
        pointers[t] = indices
        V[t] = probs[indices, range(N)]
    
    X = [0]*T
    index = np.argmax(V[T-1])
    X[T-1] = index

    for t in range(T-2, -1, -1):
        prev_index = pointers[t+1][index]
        X[t] = prev_index
        index = prev_index
        
    return X

def baum_welch(data, num_hidden_states = 4, eps = 1e-10, max_iters = 25):
    states = []
    id_dict = {}
    for seq in data:
        for s in seq:
            if s not in states:
                states.append(s)
                id_dict[s] = len(states) - 1

    T = max(len(seq) for seq in data)
    N = num_hidden_states
    K = len(states)
    
    # frequency of k value in data observations at time t
    Y_freq = np.zeros((T, K))
    for Y in data:
        for t in range(len(Y)):
            Y_freq[t,id_dict[Y[t]]] += 1

    pi = np.random.dirichlet([1]*N)
    A = np.random.dirichlet([1]*N, N)
    B = np.random.dirichlet([1]*K, N)
    
    converged = False
    iteration = 0
        
    while not converged:
        iteration += 1
        
        (alpha, beta, gamma) = np.zeros((3, T, N))
        zeta = np.zeros((N, N, T-1))
        BdotY = Y_freq.dot(B.T)
       
        # forwards
        alpha[0] = pi * BdotY[0]
        for t in range(1, T):
            alpha[t] = BdotY[t] * A.T.dot(alpha[t-1])
            alpha[t] = alpha[t] / alpha[t].sum()
        
        # backwards
        beta[T-1].fill(1.0/N)
        for t in range(T-2, -1, -1):
            beta[t] = A.dot(beta[t+1] * BdotY[t+1])
            beta[t] = beta[t] / beta[t].sum()
            
        # temporary variables
        gamma = alpha * beta
        gamma = gamma / np.sum(gamma, axis=1)[:,np.newaxis]
        zeta = np.einsum('ti,tj,ij->ijt', alpha[:T-1], (beta * BdotY)[1:], A)
        zeta = zeta / np.sum(zeta, axis = (0, 1))
       
        # new variables
        new_pi = gamma[0]
        new_A = np.sum(zeta, axis = 2) / np.sum(gamma[:-1], axis = 0)[:, np.newaxis]
       
        new_B = np.dot(gamma.T, Y_freq)
        new_B = new_B / new_B.sum(axis=1)[:,np.newaxis]  
        
        epsilon = (  np.abs(new_pi - pi).sum() / pi.size
                   + np.abs(new_A  - A).sum() / A.size
                   + np.abs(new_B  - B).sum() / B.size )
        converged = epsilon < eps or iteration == max_iters
        print('iteration ', iteration, 'change ', epsilon)
        
        pi = new_pi
        A = new_A
        B = new_B
        # end while
    
    return (pi, A, B, states, id_dict)        

np.set_printoptions(precision=16, suppress = True)    
data = load_data('/Users/alfordsimon/Desktop/Germain data/seqed50-June20.1.txt')
(pi, A, B, states, id_dict) = baum_welch(data, num_hidden_states = 10, max_iters = 50)
print(pi)
print(A)
print(B)
print(states)
Xs = viterbi(pi, A, B, states, id_dict, data[0:10])
for X, seq in zip(Xs, data):
    print(' '.join(map(str, X[0:20])))
    print('   '.join(seq[0:20]))

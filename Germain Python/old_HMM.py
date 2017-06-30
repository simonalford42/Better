#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jun 29 11:36:56 2017

@author: alfordsimon
"""
import numpy as np

def fast_baum_welch(A, B, pi, data, num_hidden_states = 4, eps = 1e-10, max_iters = 25):
    states = []
    id_dict = {}
    for seq in data:
        for s in seq:
            if s not in states:
                states.append(s)
                id_dict[s] = len(states) - 1

    T = max([len(seq) for seq in data])
    N = num_hidden_states
    K = len(states)
    
    # frequency of k value in data observations at time t
    Y_freq = np.zeros((K, T))
    for Y in data:
            for t in range(len(Y)):
               Y_freq[id_dict[Y[t]], t] += 1
    
   # pi =  np.random.dirichlet([1]*N)
   # A = np.random.dirichlet([1]*N, N)
   # B = np.random.dirichlet([1]*K, N)
    
    converged = False
    iteration = 0
        
    while not converged:
        iteration += 1
        print(iteration)
        (alpha, beta, gamma) = np.zeros((3, N, T))
        zeta = np.zeros((N, N, T-1))

        # forwards

        alpha[:,0] = pi * B.dot(Y_freq[:,0])
        
        for t in range(1, T):
            alpha[:,t] = B.dot(Y_freq[:,t]) * A.T.dot(alpha[:,t-1])
            alpha[:,t] = alpha[:,t] / sum(alpha[:,t])
        
        # backwards
    
        beta[:,T-1].fill(1.0/N)
        
        for t in range(T-2, -1, -1):
            beta[:,t] = A.dot(beta[:,t+1] * B.dot(Y_freq[:,t+1]))
            beta[:,t] = beta[:,t] / sum(beta[:,t])
            
        # temporary variables
        
        gamma = alpha * beta
        gamma = gamma / np.sum(gamma, axis=0)
        
        for t in range(T - 1):
            zeta[:,:,t] = alpha[:,t][:,np.newaxis] * A * (beta[:, t+1] * B.dot(Y_freq[:,t+1]))
            
        zeta = zeta / np.sum(zeta, axis = (0, 1))
        
        # new variables
        new_pi = gamma[:,0]
        new_A = np.sum(zeta, axis = 2) / np.sum(gamma[:,:-1], axis = 1)[:, np.newaxis]
        new_B = np.dot(gamma, Y_freq.T)
        new_B = new_B / new_B.sum(axis=1)[:,np.newaxis]  
        
        epsilon = (  np.abs(new_pi - pi).sum() / pi.size
                   + np.abs(new_A  - A).sum() / A.size
                   + np.abs(new_B  - B).sum() / B.size )
        converged = epsilon < eps or iteration == max_iters
        
        pi = new_pi
        A = new_A
        B = new_B
        # end while
    
    return (pi, A, B, states, id_dict)
    
def compare_bw(bw1, bw2, data, num_hidden_states = 4, eps = 1e-10, max_iters = 25):
    states = []
    id_dict = {}
    for seq in data:
        for s in seq:
            if s not in states:
                states.append(s)
                id_dict[s] = len(states) - 1
    N = num_hidden_states
    K = len(states)
    
    pi =  np.random.dirichlet([1]*N)
    A = np.random.dirichlet([1]*N, N)
    B = np.random.dirichlet([1]*K, N)
    
    (pi1, A1, B1, states1, id_dict1) = bw1(A, B, pi, data, num_hidden_states, eps, max_iters)
    (pi2, A2, B2, states2, id_dict2) = bw2(A, B, pi, data, num_hidden_states, eps, max_iters)    
    print(pi1 - pi2)
    print(A1 - A2)
    print(B1 - B2)

def slow_baum_welch(A, B, pi, data, num_hidden_states = 4, eps = 1e-10, max_iters = 25):
    
    states = []
    id_dict = {}
    for seq in data:
        for s in seq:
            if s not in states:
                states.append(s)
                id_dict[s] = len(states) - 1

    lengths = [len(seq) for seq in data]
    T = max(lengths)

    # convert to numeric data so that matrix indexing is easy
    num_data = [[id_dict[s] for s in seq] for seq in data]

    #print('data = ', data)
    #print('num data =', num_data)
    #print('states = ', states)
    #print('id dict = ', id_dict)
    #print('max t =', T)

    N = num_hidden_states
    K = len(states)
    num_seqs = len(data)

    #pi =  np.random.dirichlet([1]*N)
    #A = np.random.dirichlet([1]*N, N)
    #B = np.random.dirichlet([1]*K, N)
    
    converged = False
    iteration = 0
    while(not converged):
        iteration += 1
        print(iteration)
        alpha = np.zeros((N, T))
        beta = np.zeros((N, T))
        gamma = np.zeros((N, T))
        zeta = np.zeros((N, N, T-1))
    
        # forwards
        for t in range(T):
            for Y in num_data:
                if t >= len(Y):
                    continue
                
                for i in range(N):
                    if t == 0:
                        alpha[i, t] += pi[i] * B[i, Y[t]]
                    else:
                        alpha[i, t] += B[i, Y[t]] * sum([alpha[j, t-1] * A[j, i] for j in range(N)])
                        
            alpha[:,t] = alpha[:,t] / alpha[:,t].sum()
            
        # backwards
        for t in range(T-1, -1, -1):
            if t == T-1:
                beta[:,t] += np.array([num_seqs]*N)
            else:
                for Y in num_data:
                    if t+1 >= len(Y):
                        continue
                
                    for i in range(N):
                        beta[i, t] += sum([beta[j, t+1] * A[i, j] * B[j, Y[t+1]] for j in range(N)])
                        
            beta[:,t] = beta[:,t] / beta[:,t].sum()        
        
        for i in range(N):
            for t in range(T):
                gamma[i, t] = alpha[i, t] * beta[i, t]

                if t == T - 1:
                    continue
                
                for j in range(N):
                    for Y in num_data:
                        if t+1 < len(Y):
                            zeta[i, j, t] += alpha[i, t] * A[i, j] * beta[j, t+1] * B[j, Y[t+1]]
    
        gamma = gamma / np.sum(gamma, axis=0)
        zeta = zeta / np.sum(zeta, axis = (0, 1))
        
        #print(np.sum(zeta, axis=(0,1)))
        #print('gamma = \n', gamma)
        #print('zeta =\n', zeta)
        
        new_pi = gamma[:,0]
        new_A = np.sum(zeta, axis = 2) / np.sum(gamma[:,:-1], axis = 1)[:, np.newaxis]
        
        dummies = np.zeros((K, T))
        
        for Y in num_data:
            for t in range(len(Y)):
                s = Y[t]
                dummies[s, t] += 1
    
        #print('dummies = \n', dummies)
    
        new_B = np.dot(gamma, dummies.T)
        #print('proto B = \n', new_B)
        new_B = new_B / np.sum(new_B, axis=1)[:,np.newaxis]
        
        #print('new pi = \n', new_pi)
        #print('new A = \n', new_A)
        #print(new_A.sum(axis=1))
        #print('new B = \n',new_B)
        
        epsilon = np.sum(np.abs(new_pi - pi)) / pi.size
        epsilon += np.sum(np.abs(new_A - A)) / A.size
        epsilon += np.sum(np.abs(new_B - B)) / B.size
        converged = epsilon < eps or iteration == max_iters
        
        pi = new_pi
        A = new_A
        B = new_B
    
    return (pi, A, B, states, id_dict)
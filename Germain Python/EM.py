#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jun 30 09:17:52 2017

@author: alfordsimon
"""

import numpy as np
import pandas as pd
import time

class CMM():
    def __init__(self, k, ds):
        """d is a list containing the number of categories for each feature"""
        self.k = k
        self.pi = np.random.dirichlet([1]*k),
        self.alpha = [np.random.dirichlet([1]*d, size=k) for d in ds]
        self.k = k
        self.ds = ds

    def e_step(self, data):
        n, D = data.shape
        posts = np.zeros((n, self.k))
        log_posts = np.zeros((n, self.k))
        for d in range(D):
            logged_feature_probs = np.log(self.alpha[d] + 1e-10) # k by n_d
            feature_vals = data.iloc[:,d]
            dummy = pd.get_dummies(feature_vals) # n by nd
            log_output =  dummy @ logged_feature_probs.T # (n x nd) x (nd X k) = n x k
            log_posts += log_output # n x k

        log_z = np.log(self.pi + 1e-10) # k by 1
        log_posts += log_z.T
        #pre_norm ~ log_posts
        # posts ~ posterior
        posts = np.exp(log_posts)
        posts = posts / (1e-10 + np.sum(posts, axis=1)[:,None]) # n by k = p(z | x, pi, alph)

        # adds (k by 1) array to each row in posts
        log_likelihood = np.sum(log_posts*posts)

        return log_likelihood, posts

    def m_step(self, data, p_z):
        n, D = data.shape

        pi_num = np.sum(p_z, axis=0)
        new_pi = pi_num / (1e-10 + np.sum(pi_num))

        new_alpha = []

        for d in range(D):
            # p_z is n by k
            feature_vals = data.iloc[:,d]
            dummy = pd.get_dummies(feature_vals) # n by nd
            # each entry shows the weighted number of people that have that category in that cluster
            cat_pops = np.dot(p_z.T, dummy) # (k by n) x (n by nd) = (k by nd)
            # now we have to average the category for each cluster so that these piles turn into probabilities
            cat_pops = cat_pops / (1e-10 + np.sum(cat_pops, axis = 1)[:,None])
            new_alpha.append(cat_pops)

        return new_pi, new_alpha
        
    def fit(self, data, eps=1e-4, verbose=True, max_iters=100):
        """ Fits the model to data
        data - an NxD pandas DataFrame
        eps - the tolerance for the stopping criterion
        verbose - whether to print ll every iter
        max_iters - maximum number of iterations before giving up

        returns a boolean indicating whether fitting succeeded

        if fit was successful, sets the following properties on the Model object:
          n_train - the number of data points provided
          max_ll - the maximized log-likelihood
        """
        last_ll = np.finfo(float).min
        start_t = last_t = time.time()
        
        i = 0
        converged = False
        while i < max_iters and not converged:
            i += 1
                
            ll, p_z = self.e_step(data)
            new_pi, new_alpha = self.m_step(data, p_z)
            self.pi = new_pi
            self.alpha = new_alpha
            
            if verbose:
                dt = time.time() - last_t
                last_t += dt
                print('iter %s: ll = %.5f  (%.2f s)' % (i, ll, dt))
            
            if abs((ll - last_ll) / ll) < eps:
                converged = True
                
            last_ll = ll

        setattr(self, 'n_train', len(data))
        setattr(self, 'max_ll', ll)
        self.params.update({'p_z': p_z})

        print('max ll = %.5f  (%.2f min, %d iters)' %
              (ll, (time.time() - start_t) / 60, i))

        return True

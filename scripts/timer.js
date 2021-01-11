/**
 * A standard timer for Java script.
 * @author: Mover Zhou 
 * @url http://development.ingod.asia
 * 
 * Version: 1.0
 * Updated: November 9th, 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

if (!Function.prototype.bind) {
  Function.prototype.bind = function(oThis) {
    if (typeof this !== 'function') {
      // closest thing possible to the ECMAScript 5
      // internal IsCallable function
      throw new TypeError('Function.prototype.bind - what is trying to be bound is not callable');
    }

    var aArgs   = Array.prototype.slice.call(arguments, 1),
        fToBind = this,
        fNOP    = function() {},
        fBound  = function() {
          return fToBind.apply(this instanceof fNOP
                 ? this
                 : oThis,
                 aArgs.concat(Array.prototype.slice.call(arguments)));
        };

    fNOP.prototype = this.prototype;
    fBound.prototype = new fNOP();

    return fBound;
  };
}
// Above code was copied from mozilla community.
var Timer = function() {
  if (arguments.length > 1) {
    this.limit = arguments[0];
    this.action = typeof (arguments[1]) == 'undefined' ? function() {
    } : arguments[1];
  } else {
    this.limit = 1;
    this.action = arguments[0];
  }

  list = [ 'started', 'running', 'stopped', 'completed' ];
  this.timer = null;
  this.times = 0;
  this.end = 0;
  this.done = null;
  this.status = list[0];

  this.setAction = function() {
    this.action = arguments[0];
  };

  this.start = function() {
    if (this.timer) {
      this.stop();
    }

    if (this.status != list[3]) {
      this.timer = setTimeout(this.action.bind(this), this.limit * 1000);
      this.times++;
      this.status = list[1];
    }

    return this;
  };

  this.stop = function() {
    if (typeof (arguments[0]) == 'number') {
      this.end = arguments[0];
      return this;
    }
    
    clearTimeout(this.timer);
    this.timer = null;
    this.status = list[2];
    
    if (this.times > 0 && this.times === this.end) {
    	this.times = 0;
      this.complete();
    }

    return this;
  };

  this.complete = function() {
    if (typeof (arguments[0]) == 'function') {
      this.done = arguments[0];
      return this;
    }

    if (typeof (this.done) == 'function') {
      this.done();
      this.status = list[3];
    }
  };

  this.status = function() {
    return this.status;
  };

}

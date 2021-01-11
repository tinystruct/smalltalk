/**
 * A standard timer for Java script.
 * 
 * @author: Mover Zhou
 * @url http://development.ingod.asia
 * 
 * Version: 1.0 Updated: November 9th, 2011
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
var struct = {
	baseId : "__base__"
};

struct.attachOnLoadEvent = function() {
	if (typeof window.addEventListener != "undefined") {
		window.addEventListener("load", arguments[0], false);
	} else if (typeof window.attachEvent != "undefined") {
		window.attachEvent("onload", arguments[0]);
	} else {
		if (window.onload != null) {
			var _attach = arguments[0];
			var _onloaded = window.onload;
			window.onload = function(e) {
				_onloaded(e);
				window[_attach]();
			};
		} else {
			window.onload = arguments[0];
		}
	}
}

struct.attachOnLoadEvent(function() {
	if (typeof (document.getElementById(struct.baseId)) == 'undefined') {
		struct.base = document.createElement("DIV");
		struct.base.setAttribute("id", struct.baseId);
		struct.base.setAttribute("class", struct.baseId);
		struct.base.style.display = "none";
		document.body.appendChild(struct.base);
	}
	else
		struct.base = document.getElementById(struct.baseId);
});

struct.message = function() {
	this.panel = struct.base?struct.base:document.getElementById(struct.baseId);

	var $this_message = this;
	this.timer = new Timer(3, function() {
		$this_message.hide();
	});

	this.showMessage = function() {
		this.panel.innerHTML = arguments[0];
		this.panel.style.cssText = "padding-left:2px;padding-right:2px;background:#AF3233;color:#ffffff;font-size:12px;text-align:center";
		this.panel.style.display = 'block';
		this.timer.start();
		struct.fixed(this.panel, 0, 0);
	};
	this.hide = function() {
		this.panel.innerHTML = '';
		this.panel.style.display = 'none';

		this.timer.stop();
	};
}

struct.Event = {
	KEY : {
		DOWN : "keydown",
		UP : "keyup"
	},
	MOUSE : {
		DOWN : "mousedown",
		UP : "mouseup",
		CLICK : "click",
		DBLCLICK : "dblclick",
		OVER : "mouseover",
		OUT : "mouseout"
	},
	ELEMENT : {
		LOAD : "load",
		UNLOAD : "unload"
	}
}

struct.setAttribute = function(element, name, value) {
	if (typeof (element.addEventListener) != "undefined")
		element[name] = value;
	else
		element.setAttribute(name, value);
}

struct.getAttribute = function(element, name) {
	if (typeof (element.addEventListener) != "undefined")
		return element.attributes[name].nodeValue;
	else
		element.getAttribute(name);
}

/*
 * attachEvent(element,event,func)
 */
struct.attachEvent = function(element, event, func, attributes) {
	if (typeof (element.addEventListener) != "undefined") {
		element.addEventListener(event, func, false);
	} else if (typeof (element.attachEvent) != "undefined") {
		element.attachEvent("on" + event, func);
	}
}

struct.save = function(name, value) {
	var argv = arguments;
	var argc = arguments.length;
	var expires = (argc > 2) ? argv[2] : null;
	var path = (argc > 3) ? argv[3] : '/';
	var domain = (argc > 4) ? argv[4] : null;
	var secure = (argc > 5) ? argv[5] : false;
	document.cookie = name + "=" + escape(value)
			+ ((expires == null) ? "" : ("; expires=" + expires))
			+ ((path == null) ? "" : ("; path=" + path))
			+ ((domain == null) ? "" : ("; domain=" + domain))
			+ ((secure == true) ? "; secure" : "");
};

struct.get = function(name) {
	var arg = name + "=";
	var alen = arg.length;
	var clen = document.cookie.length;
	var i = 0;
	var j = 0;
	while (i < clen) {
		j = i + alen;
		if (document.cookie.substring(i, j) == arg)
			return struct.getValue(j);
		i = document.cookie.indexOf(" ", i) + 1;
		if (i == 0)
			break;
	}
	return null;
};

struct.clear = function(name) {
	if (struct.get(name)) {
		var expdate = new Date();
		expdate.setTime(expdate.getTime() - (86400 * 1000 * 1));
		struct.set(name, "", expdate);
	}
};

struct.getValue = function(offset) {
	var endstr = document.cookie.indexOf(";", offset);
	if (endstr == -1) {
		endstr = document.cookie.length;
	}
	return unescape(document.cookie.substring(offset, endstr));
};

struct.dateFormat = function(date, format) {
	if (!format)
		format = 'yyyy-mm-dd HH:MM:ss';

	if (date instanceof Date) {
		return date.format(format);
	} else {
		return struct.parseDate(date).format(format);
	}
};

struct.parseDate = function() {
	if (typeof arguments[0] == 'string') {
		var results = arguments[0].match(/^ *(\d{4})-(\d{1,2})-(\d{1,2}) *$/);

		if (results && results.length > 3)
			return new Date(parseInt(results[1]), parseInt(results[2]) - 1,
					parseInt(results[3]));

		results = arguments[0]
				.match(/^ *(\d{4})-(\d{1,2})-(\d{1,2}) +(\d{1,2}):(\d{1,2}):(\d{1,2}) *$/);
		if (results && results.length > 6)
			return new Date(parseInt(results[1]), parseInt(results[2]) - 1,
					parseInt(results[3]), parseInt(results[4]),
					parseInt(results[5]), parseInt(results[6]));

		results = arguments[0]
				.match(/^ *(\d{4})-(\d{1,2})-(\d{1,2}) +(\d{1,2}):(\d{1,2}):(\d{1,2})\.(\d{1,9}) *$/);
		if (results && results.length > 7)
			return new Date(parseInt(results[1]), parseInt(results[2]) - 1,
					parseInt(results[3]), parseInt(results[4]),
					parseInt(results[5]), parseInt(results[6]),
					parseInt(results[7]));
	}

	return new Date(Date.parse(arguments[0]));
}

struct.loading = function() {
	this.id = "loading";
	this.icon = document.getElementById(this.id);
	this.element = typeof (arguments[0]) != undefined ? arguments[0]
			: document.body;
	this.start = function() {
		if (!this.icon) {
			var icon = document.createElement("img");
			icon.setAttribute("src", "/themes/images/loading.gif");
			icon.setAttribute("id", "loading");
			this.element.appendChild(icon);
		} else
			this.icon.style.display = 'block';

		this.element.style.display = 'block';
	}, this.stop = function() {
		if (this.element)
			this.element.style.display = 'none';
	}
};

/*
 * 感谢网友淡新举
 */
var inited = false;
struct.fixed = function(element, top, left) {

	this.inited = inited;
	element.style.display = "block";
	if (!window.XMLHttpRequest && window.ActiveXObject) {
		element.style.position = "absolute";

		this.setGlobal();
	} else {
		element.style.position = "fixed";
	}

	element.style.top = top + "px";
	element.style.left = left + "px";

	this.addCSSRule = function(key, value) {
		var css = document.styleSheets[document.styleSheets.length - 1];
		css.cssRules ? (css.insertRule(key + "{" + value + "}",
				css.cssRules.length)) : (css.addRule(key, value));
	};

	this.setGlobal = function() {
		if (window.navigator.appVersion.indexOf("IE 6.0") == -1)
			if (!this.inited) {
				document.body.style.height = "100%";
				document.body.style.overflow = "auto";
				this.addCSSRule("*html", "overflow-x:auto;overflow-y:hidden;");
				inited = this.inited = true;
			}
	};

};

struct.getStyle = function(className, attr) {

	var CSSSheets, value = "", currentRule;
	CSSSheets = document.styleSheets;

	for (j = 0; j < CSSSheets.length; j++) {
		for (i = 0; i < CSSSheets[j].cssRules.length; i++) {
			currentRule = CSSSheets[j].cssRules[i].selectorText;

			if (currentRule && currentRule.indexOf(className) != -1)
				if (document.querySelectorAll(currentRule).length) {
					return CSSSheets[j].cssRules[i].style[attr];
				}
		}
	}

	return value;
}
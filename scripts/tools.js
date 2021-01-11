/*
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */

var dateFormat = function () {
	var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
		timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
		timezoneClip = /[^-+\dA-Z]/g,
		pad = function (val, len) {
			val = String(val);
			len = len || 2;
			while (val.length < len) val = "0" + val;
			return val;
		};

	// Regexes and supporting functions are cached through closure
	return function (date, mask, utc) {
		var dF = dateFormat;

		// You can't provide utc if you skip other args (use the "UTC:" mask prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}

		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;
		if (isNaN(date)) throw SyntaxError("invalid date");

		mask = String(dF.masks[mask] || mask || dF.masks["default"]);

		// Allow setting the utc argument via the mask
		if (mask.slice(0, 4) == "UTC:") {
			mask = mask.slice(4);
			utc = true;
		}

		var	_ = utc ? "getUTC" : "get",
			d = date[_ + "Date"](),
			D = date[_ + "Day"](),
			m = date[_ + "Month"](),
			y = date[_ + "FullYear"](),
			H = date[_ + "Hours"](),
			M = date[_ + "Minutes"](),
			s = date[_ + "Seconds"](),
			L = date[_ + "Milliseconds"](),
			o = utc ? 0 : date.getTimezoneOffset(),
			flags = {
				d:    d,
				dd:   pad(d),
				ddd:  dF.i18n.dayNames[D],
				dddd: dF.i18n.dayNames[D + 7],
				m:    m + 1,
				mm:   pad(m + 1),
				mmm:  dF.i18n.monthNames[m],
				mmmm: dF.i18n.monthNames[m + 12],
				yy:   String(y).slice(2),
				yyyy: y,
				h:    H % 12 || 12,
				hh:   pad(H % 12 || 12),
				H:    H,
				HH:   pad(H),
				M:    M,
				MM:   pad(M),
				s:    s,
				ss:   pad(s),
				l:    pad(L, 3),
				L:    pad(L > 99 ? Math.round(L / 10) : L),
				t:    H < 12 ? "a"  : "p",
				tt:   H < 12 ? "am" : "pm",
				T:    H < 12 ? "A"  : "P",
				TT:   H < 12 ? "AM" : "PM",
				Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
				o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
				S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
			};

		return mask.replace(token, function ($0) {
			return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
		});
	};
}();

// Some common format strings
dateFormat.masks = {
	"default":      "ddd mmm dd yyyy HH:MM:ss",
	shortDate:      "m/d/yy",
	mediumDate:     "mmm d, yyyy",
	longDate:       "mmmm d, yyyy",
	fullDate:       "dddd, mmmm d, yyyy",
	shortTime:      "h:MM TT",
	mediumTime:     "h:MM:ss TT",
	longTime:       "h:MM:ss TT Z",
	isoDate:        "yyyy-mm-dd",
	isoTime:        "HH:MM:ss",
	isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
	isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
	dayNames: [
		"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	],
	monthNames: [
		"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
	return dateFormat(this, mask, utc);
};

var Tips=function()
{
	this.id=typeof(arguments[0])!='undefined'?arguments[0].id:"$__TIPS";
	this.message=typeof(arguments[0])!='undefined'?arguments[0].message:'';
	this.panel={};
	
	this.getPanel=function(){
		return this.panel;
	};
	if(!document.getElementById(this.id))
	{
		this.panel=document.createElement("div");
		this.panel.setAttribute("id",this.id);
		this.panel.setAttribute("style","padding:3px; border: 1px solid #a3a3a3;	-moz-border-radius: 3px;	-khtml-border-radius: 3px;	-webkit-border-radius: 3px;	border-radius: 3px;background:#fff;");
		this.panel.style.display="none";
		
		document.body.appendChild(this.panel);
	}
	else
	{
		this.panel=document.getElementById(this.id);
	}
	
	this.sameContent=true;
	
	this.onshow=function(){};
	this.onhide=function(){};
	
	if(typeof(arguments[0])!='undefined')
	{
		this.onshow=typeof(arguments[0].onshow)=='undefined'?function(){}:arguments[0].onshow;
		this.onhide=typeof(arguments[0].onhide)=='undefined'?function(){}:arguments[0].onhide;
	}
	
	var $this_tips=this;
	this.timer=new Timer(2,function(){$this_tips.hide();});
	
	this.setId=function(id)
	{
		this.id=id;
		this.panel=document.getElementById(this.id);
		this.panel.setAttribute("style","display:none;	padding:3px; border: 1px solid #a3a3a3;	-moz-border-radius: 3px;	-khtml-border-radius: 3px;	-webkit-border-radius: 3px;	border-radius: 3px;background:#fff;");
	};
	this.getId=function()
	{
		return this.id;
	};
	this.append=function(item)
	{
		this.panel.appendChild(item);
		this.sameContent=false;
	};
	this.update=function(item)
	{
		this.panel.innerHTML='';
		this.panel.appendChild(item);
		//this.panel.innerHTML+="<div style=\"clear:both\"></div>";
	};
	this.style=function()
	{
		if(typeof(arguments[0])!='undefined')
		this.panel.setAttribute("style",arguments[0]);
	};
	this.show=function(){
		var event,element,message,top=0,left=0;
		if(typeof(arguments[0])=='undefined')
		{
			alert('Event Object is null!');
			return;
		}
		else
			event=arguments[0];
		
		if(!event.target)
		{
			event=window.event;
			element=currentElement=event.srcElement;
			
			do
			{
				top+=element.offsetTop;
				left+=element.offsetLeft;
			}
			while(element=element.offsetParent);
		}
		else
		{
			currentElement=event.target;
			top=currentElement.offsetTop;
			left=currentElement.offsetLeft;
		}

		//alert(target);
		if(typeof(arguments[1])!='undefined')
		{
			message=arguments[1];
			if(this.message!=message)
			{
				this.sameContent=false;
				this.message=message;
			}
		}
		
		this.panel.style.top=(top+currentElement.offsetHeight)+"px";
		this.panel.style.left=left+"px";
		this.panel.style.position="absolute";
		this.panel.style.zIndex="1000";
		this.panel.style.display="block";
		
		this.onshow();

		this.timer.start();
		var $this=this;
		struct.attachEvent(this.panel,"mouseover",function(){
			$this.timer.stop();
		});
		
		struct.attachEvent(this.panel,"mouseout",function(){
			$this.timer.start();
		});
	};
	this.hide=function(){
		this.panel.style.display="none";
		this.onhide();
		this.timer.stop();
	};
}

var Menu=function(){
	this.items=document.createElement("ul");
	this.id=(typeof(arguments[0])=='undefined'?'menu':arguments[0]);
	this.panel=function(){
		if(!document.getElementById(this.id))
		{
			var $panel=document.createElement("div");
			$panel.setAttribute("id",this.id);
			$panel.style.display="none";
			
			document.body.appendChild($panel);
			return $panel;
		}
		
		return document.getElementById(this.id);
	}
	
	$_this_menu=this;
	this.timer=new Timer(1, function(){
		$_this_menu.hide();
	});
	
	this.itemlist=new Array();
		
	this.appendItem=function(){
		var currentItem;
		if(arguments[0]=='undefined')
		{
			alert("the arguments cannot be null!");
			return;
		}
		else
		{
			currentItem=arguments[0];
		}
		
		var o=new Object();
			o.caption=currentItem.caption;
			o.action=currentItem.action;
		
		this.itemlist[this.itemlist.length]=o;
	}
	
	this.show=function(){
		var event,top=0,left=0,element,sid;
		
		if(typeof(arguments[0])=='undefined')
		{
			alert("the arguments[0] cannot be null!");
			return;
		}
		else
		{
			event=arguments[0];
		}
		
		if(typeof(arguments[1])=='undefined')
		{
			alert("the arguments[1] cannot be null!");
			return;
		}
		else
		{
			sid=arguments[1];
		}
		
		if(window.event)
		{
			event=window.event;
			element=currentElement=event.srcElement;
			
			do
			{
				top+=element.offsetTop;
				left+=element.offsetLeft;
			}
			while(element=element.offsetParent);
		}
		else
		{
			element = currentElement = event.target;
			
			if(currentElement.offsetParent!=null && struct.getStyle("."+currentElement.offsetParent.className,"position")=="absolute")
			{
				do
				{
					top+=element.offsetTop;
					left+=element.offsetLeft;
				}
				while(element=element.offsetParent);
			}
			else {
				top=currentElement.offsetTop;
				left=currentElement.offsetLeft;
			}
			
		}

		var text="";
		for(var i=1;i<currentElement.parentNode.childNodes.length;i++)
		{
			if(currentElement.parentNode.childNodes[i].tagName=="SPAN")
			text+=(currentElement.parentNode.childNodes[i].childNodes[0].nodeValue)
			else
			text+=(currentElement.parentNode.childNodes[i].nodeValue);
		}
		
		text=text.replace(/'/g,"\\'");
		this.items=document.createElement("ul");
		for(var i=0;i<this.itemlist.length;i++)
		{
			var item=document.createElement("li");
			var link=document.createElement("a");
				link.href=this.itemlist[i].action.replace('$text',text).replace('$id',sid);
				link.appendChild(document.createTextNode(this.itemlist[i].caption));
				
				item.appendChild(link);
			this.items.appendChild(item);
		}
		
		this.panel().innerHTML="";
		this.panel().appendChild(this.items);
		//this.panel.update(this.items);
		this.panel().style.position="absolute";
		this.panel().style.top=(top+currentElement.offsetHeight)+"px";
		this.panel().style.left=left+"px";
		this.panel().style.display="block";
		
		var $this=this;
		var el=$this.panel();
		
		struct.attachEvent(
			el,
			struct.Event.MOUSE.OVER,
			function()
			{
				$this.timer.stop();
			}
		);
		
		struct.attachEvent(
				el,
				struct.Event.MOUSE.OUT,
				function()
				{
					$this.timer.start();
				}
		);
		
		this.timer.start();
	}
	this.hide=function(){
		this.panel().style.display="none";
		this.timer.stop();
		return true;
	}
}

var DTimer=function()
{
	if (arguments.length > 1) {
		this.limit = arguments[0];
		this.action = typeof (arguments[1]) == 'undefined' ? function() {
		} : arguments[1];
	} else {
		this.limit = 1;
		this.action = arguments[0];
	}
	
	this.timer=null;
	
	this.setAction=function()
	{
		this.action=arguments[0];
	};
	this.start=function(){
		if(this.timer)this.stop();
		this.timer=setTimeout(this.action,this.limit*1000);
//		document.title="started";
	};
	this.stop=function(){
		clearTimeout(this.timer);
//		document.title="stopped";	
	};
	this.excute=function()
	{
		this.action();
	};
}
var Dialog=function(){
	this.id='dialog';
	this.sid=-1;
	this.title='Default Title';
	this.width=300;
	this.height=0;
	this.action=null;
	this.panel=document.createElement("div");
	this.value='';
	
	var $this_dialog=this;
	this.timer=new Timer(0.1,function(){$this_dialog.reposition();});
	
	if(typeof(arguments[0])!='undefined'){
		this.id=typeof(arguments[0].id)!='undefined'?arguments[0].id:'dialog';
		this.title=typeof(arguments[0].title)!='undefined'?arguments[0].title:this.title;
		this.width=typeof(arguments[0].width)!='undefined'?arguments[0].width:this.width;
		this.height=typeof(arguments[0].height)!='undefined'?arguments[0].height:this.height;
		this.action=typeof(arguments[0].action)!='undefined'?arguments[0].action:this.action;
		
		this.okLabel=typeof(arguments[0].okLabel)!='undefined'? arguments[0].okLabel : "确定";
		this.cancelLabel=typeof(arguments[0].cancelLabel)!='undefined'? arguments[0].cancelLabel : "取消";
	}
	
	this.show=function(){
		this.parameters=typeof(arguments[0]!='undefined')?arguments[0]:{};
		
		var dialog=document.createElement('ul');
		var dialog_title=document.createElement('li');
			dialog_title.appendChild(document.createTextNode(this.title));
			dialog.appendChild(dialog_title);
			
		var dialog_textarea=document.createElement('li');
		var textarea=document.createElement('textarea');
			textarea.setAttribute("cols",48);
			textarea.setAttribute("rows",5);
			textarea.setAttribute("id","content");
			textarea.setAttribute("name","content");
			textarea.appendChild(document.createTextNode(this.parameters.value?this.parameters.value:''));
			dialog_textarea.appendChild(textarea);
			dialog.appendChild(dialog_textarea);
			
		var dialog_control=document.createElement('li');
			dialog_control.setAttribute("style","text-align:right");
		var	save_button=document.createElement("a");
			save_button.href="javascript:void(0)";
			save_button.appendChild(document.createTextNode(this.okLabel));
			
		var	close_button=document.createElement("a");
			close_button.href="javascript:void(0)";
			close_button.setAttribute("style","margin-left:10px;");
			close_button.appendChild(document.createTextNode(this.cancelLabel));
			
			dialog_control.appendChild(save_button);
			dialog_control.appendChild(close_button);
		
		var $this=this;
			struct.attachEvent(save_button,"click",function(){$this.save()});
			struct.attachEvent(close_button,"click",function(){$this.close()});
			
			dialog.appendChild(dialog_control);
			this.panel.innerHTML='';
			this.panel.appendChild(dialog);
			
			this.panel.style.display="block";
			this.panel.setAttribute("id","dialog");
			this.panel.setAttribute("onmousedown","");
			this.panel.setAttribute("ondragstart","");
			
			document.body.appendChild(this.panel);
			this.reposition();
	};
	this.close=function(){
		this.panel.style.display='none';
		this.timer.stop();
	};
	this.reposition=function(){
		var x=0,y=0;
		this.panel.style.position="absolute";

		var ie=(document.compatMode && document.compatMode != "BackCompat")? document.documentElement : document.body;
		if(!document.all)
		{
			x-=window.pageXOffset;
			y-=window.pageYOffset;
		}
		
		this.panel.style.right=x+"px";
		this.panel.style.bottom=y+"px";
		
		this.timer.start(1);
	};
	this.save=function(){
		if(this.action!=null && typeof(this.action)=='function')
		{
			this.action();
			
			return;
		}
		else
		{
			alert('请设定功能！');
			return;
		}
	}
}

var TDialog=function(){
	this.id='tdialog';
	this.sid=-1;
	this.title='Default Title';
	this.width=300;
	this.height=0;
	this.action=null;
	this.panel=document.createElement("div");
	this.value='';
	
	var $this_dialog=this;
	this.timer=new Timer(0.1,function(){$this_dialog.reposition();});
	
	if(typeof(arguments[0])!='undefined'){
		this.id=typeof(arguments[0].id)!='undefined'?arguments[0].id:'tdialog';
		this.title=typeof(arguments[0].title)!='undefined'?arguments[0].title:this.title;
		this.width=typeof(arguments[0].width)!='undefined'?arguments[0].width:this.width;
		this.height=typeof(arguments[0].height)!='undefined'?arguments[0].height:this.height;
		this.action=typeof(arguments[0].action)!='undefined'?arguments[0].action:this.action;
		
		this.okLabel=typeof(arguments[0].okLabel)!='undefined'? arguments[0].okLabel : "确定";
		this.cancelLabel=typeof(arguments[0].cancelLabel)!='undefined'? arguments[0].cancelLabel : "取消";
	}
	
	this.show=function(){
		this.html=typeof(arguments[0]!='undefined')?arguments[0]:"";
		
		var dialog=document.createElement('ul');
		var dialog_title=document.createElement('li');
			dialog_title.appendChild(document.createTextNode(this.title));
			dialog.appendChild(dialog_title);
			
		var dialog_textarea=document.createElement('li');
		var textarea=document.createElement('DIV');
			textarea.setAttribute("class","text");
			textarea.innerHTML=(this.html);
			
			dialog_textarea.appendChild(textarea);
			dialog.appendChild(dialog_textarea);
			
		var dialog_control=document.createElement('li');
			dialog_control.setAttribute("style","text-align:right");
		var	save_button=document.createElement("a");
			save_button.href="javascript:void(0)";
			save_button.appendChild(document.createTextNode(this.okLabel));
			
		var	close_button=document.createElement("a");
			close_button.href="javascript:void(0)";
			close_button.setAttribute("style","margin-left:10px;");
			close_button.appendChild(document.createTextNode(this.cancelLabel));
			
			dialog_control.appendChild(save_button);
			dialog_control.appendChild(close_button);
		
		var $this=this;
			struct.attachEvent(save_button,"click",function(){$this.save()});
			struct.attachEvent(close_button,"click",function(){$this.close()});
			
			dialog.appendChild(dialog_control);
			this.panel.innerHTML='';
			this.panel.appendChild(dialog);
			
			this.panel.style.display="block";
			this.panel.setAttribute("id","tdialog");
			this.panel.setAttribute("onmousedown","");
			this.panel.setAttribute("ondragstart","");
			
			document.body.appendChild(this.panel);
			this.reposition();
	};
	this.close=function(){
		this.panel.style.display='none';
		this.timer.stop();
	};
	this.reposition=function(){
		var x=0,y=0;
		this.panel.style.position="absolute";

		var ie=(document.compatMode && document.compatMode != "BackCompat")? document.documentElement : document.body;
		if(!document.all)
		{
			x-=window.pageXOffset;
			y-=window.pageYOffset;
		}
		
		this.panel.style.right=x+"px";
		this.panel.style.bottom=y+"px";
		
		this.timer.start(1);
	};
	this.save=function(){
		if(this.action!=null && typeof(this.action)=='function')
		{
			this.action();
			
			return;
		}
		else
		{
			alert('请设定功能！');
			return;
		}
	}
}

function copyToClipboard(txt) {       
     if(window.clipboardData) {       
              window.clipboardData.clearData();       
              window.clipboardData.setData("Text", txt);
              alert("复制成功！");          
      } else if(navigator.userAgent.indexOf("Opera") != -1) {       
           window.location = txt;       
      } else if (window.netscape) {       
		  try 
		  {       
		        netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");       
		   } catch (e) {       
		        alert("被浏览器拒绝！\n请在浏览器地址栏输入'about:config'并回车\n然后将'signed.applets.codebase_principal_support'设置为'true'");       
		   }       
          var clip = Components.classes['@mozilla.org/widget/clipboard;1'].createInstance(Components.interfaces.nsIClipboard);       
          if (!clip)       
               return;       
          var trans = Components.classes['@mozilla.org/widget/transferable;1'].createInstance(Components.interfaces.nsITransferable);       
          if (!trans)       
               return;       
           trans.addDataFlavor('text/unicode');       
          var str = new Object();       
          var len = new Object();       
          var str = Components.classes["@mozilla.org/supports-string;1"].createInstance(Components.interfaces.nsISupportsString);       
          var copytext = txt;       
           str.data = copytext;       
           trans.setTransferData("text/unicode",str,copytext.length*2);       
          var clipid = Components.interfaces.nsIClipboard;       
          if (!clip)       
               return false;       
           clip.setData(trans,null,clipid.kGlobalClipboard);       
           alert("复制成功！");       
      }       
}

function subscribe()
{
	var email=document.getElementById('toemail').value;
	var bible=document.getElementById('bible').value, article=document.getElementById('article').value;
	var submit=document.getElementById('subscribe_button');
	var loading=new struct.loading(document.getElementById('subscribe-message'));
	if(email && submit)
	{
		submit.disabled="disabled";
		
		loading.start();
		$.ajax({url:"?q=services/subscribe",data:{toemail:email,bible:bible,article:article}})
		.success(function(data,textStatus,transport){
				loading.stop();
				if(transport.responseText.indexOf("true")==0)
				{
					new struct.message().showMessage("订阅成功！");
				}
				else if(transport.responseText.indexOf("false")==0)
				{
					new struct.message().showMessage("订阅失败！");
				}
				else if(transport.responseText.indexOf("invalid")==0)
				{
					new struct.message().showMessage("邮件地址不正确！例如：example@ingod.asia");
				}
				else if(transport.responseText.indexOf("error")==0)
				{
					new struct.message().showMessage("无法发送邮件，请检查是否正确连接到邮箱服务器！");
				}
				else if(transport.getResponseHeader("Status")=="0000")
				{
					new struct.message().showMessage("你没有足够的权限执行此项操作！");
				}
				else if(transport.responseText.indexOf('<')==0)
				{
					new struct.message().showMessage("你需要登录以后才可以使用此项功能！");
				}
				else
				{
					new struct.message().showMessage("邮件服务目前比较忙，无法发送邮件，请稍后重试！");
				}
				
				submit.disabled="";
			}).error(function(transport){
				submit.disabled="";
				loading.stop();
			});
	}
	return false;
}

function invite()
{
	var email=document.getElementById('email').value;
	var submit=document.getElementById('invite_button');
	var loading=new struct.loading(document.getElementById('invite-message'));
	if(email && submit)
	{
		submit.disabled="disabled";
		
		loading.start();
		$.ajax({
			url:"?q=friends/invite",
			data:{mailto:email}
		}).error(function(){
				submit.disabled="";
				loading.stop();
		}).success(function(data,textStatus,transport){
				loading.stop();
				if(data.indexOf("true")==0)
				{
					new struct.message().showMessage("邀请成功！");
				}
				else if(data.indexOf("false")==0)
				{
					new struct.message().showMessage("邀请失败！");
				}
				else if(data.indexOf("invalid")==0)
				{
					new struct.message().showMessage("邮件地址不正确！例如：example@ingod.asia");
				}
				else if(data.indexOf("error")==0)
				{
					new struct.message().showMessage("无法发送邮件，请检查是否正确连接到邮箱服务器！");
				}
				else if(transport.getResponseHeader("Status")=="0000")
				{
					new struct.message().showMessage("你没有足够的权限执行此项操作！");
				}
				else if(data.indexOf('<')==0)
				{
					new struct.message().showMessage("你需要登录以后才可以使用此项功能！");
				}
				else
				{
					new struct.message().showMessage("邮件服务目前比较忙，无法发送邮件，请稍后重试！");
				}
				
				submit.disabled="";
			});
	}
	return false;
}

var ImageBox=function(){
	this.start=typeof(arguments[0])!=undefined?arguments[0].start:0;
	this.end=typeof(arguments[0])!=undefined?arguments[0].end:0;
	this.panel=typeof(arguments[0])!=undefined?arguments[0].panel:document.createElement("div");
	
	this.pagesize=21;
	this.image_list=null;
	
	this.click=typeof(arguments[0])!=undefined?arguments[0].click:function(){alert("Undefined Function!")};
	this.close=typeof(arguments[0])!=undefined?arguments[0].onclose:function(){alert("Undefined Function!")};
	
	this.selectedImage="";
	this.hide=function(){
		this.panel.style.display="none";
	};
	this.getImages=function()
	{
		var $this=this;
		
		this.image_list=document.createElement("table");
		var image_line=document.createElement("tr");
		var size=this.start+this.pagesize;
		
			if(size>this.end)size=this.end;
			for(var i=this.start;i<size;i++)
			{
				var icon_frame=document.createElement("a");

				var icon=document.createElement("img");
				icon.setAttribute("src","/template/images/faces/"+(i+1)+".png");
				icon.setAttribute("style","border:1px solid #f5f5f5;width:70px;cursor:pointer");
				icon.setAttribute("class","icon");
				
				struct.attachEvent(icon,"click",function(){
					$this.selectedImage=this.src;
					$this.click(this.src);
				});
				icon_frame.appendChild(icon);

				var td=document.createElement("td");
				td.appendChild(icon_frame);

				if(i%7==0)
				{
					image_line=document.createElement("tr");
					this.image_list.appendChild(image_line);
				}
				
				image_line.appendChild(td);
			}
		
		var page_controller=document.createElement("tr");
		
			page_button_panel=document.createElement("td");
			page_button_panel.setAttribute("colspan",7);
			
		var page_next_button=document.createElement("a");
			page_next_button.innerHTML=("下一页");
			page_next_button.setAttribute("href","javascript:void(0)");
			page_next_button.setAttribute("style","font-size:12px;padding:3px");
	
		var page_back_button=document.createElement("a");
			page_back_button.innerHTML=("上一页");
			page_back_button.setAttribute("href","javascript:void(0)");
			page_back_button.setAttribute("style","font-size:12px;padding:3px");
			
		var page_close_button=document.createElement("a");
			page_close_button.innerHTML=("关闭");
			page_close_button.setAttribute("href","javascript:void(0)");
			page_close_button.setAttribute("style","float:right;font-size:12px;padding:3px");
			
			struct.attachEvent(page_close_button,"click",function(){
				$this.close($this.selectedImage);
				$this.hide();
			});
		
			if(size<this.end)
			{
				struct.attachEvent(page_next_button,"click",function(event){
					$this.image_list=new ImageBox({start:$this.start>$this.end?0:$this.start+$this.pagesize,end:$this.end,panel:$this.panel,click:$this.click,onclose:$this.close}).getImages();
					$this.panel.innerHTML='';
					$this.panel.appendChild($this.image_list);
				});
			}
			
			struct.attachEvent(page_back_button,"click",function(event){
				$this.image_list=new ImageBox({start:($this.start-$this.pagesize)<0?0:$this.start-$this.pagesize,end:$this.end,panel:$this.panel,click:$this.click,onclose:$this.close}).getImages();
				$this.panel.innerHTML='';
				$this.panel.appendChild($this.image_list);
			});

			page_button_panel.appendChild(page_back_button);
			page_button_panel.appendChild(page_next_button);
			page_button_panel.appendChild(page_close_button);

			page_controller.appendChild(page_button_panel);
			
			this.image_list.appendChild(page_controller);
			
			return this.image_list;
	}
}
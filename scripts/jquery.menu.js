/**
 * A slide Menu plugin for Jquery.
 * @author: Mover Zhou 
 * @url http://development.ingod.asia
 * 
 * @depends jquery.level.js, timer.js
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

(function($){
	$.settings = {
		defaultHandler: "mouseover"
	};
	
	$.fn.start = function(func){
		
		if($(this).data("timer")) {
			$(this).data("timer").start();
		}
		else {
			$this = $(this);
			$timer = new Timer(0.1,function(){
				func();
			}).start();
			
			$this.data("timer", $timer);
		}
	}
	
	$.fn.stop = function(){
		if($(this).data("timer")) {
			$(this).data("timer").stop();
		}
	}
	
	$.fn.renderMenu = function(){
		
		if($(this).data("rendered")==true){
			$(this).find('> li > a').each(function(i){
				if($(this).next("ul").length>0) {
					$(this).siblings().hide();
					
					if($(this).siblings().level() > 2)
					$(this).siblings().css({"margin-left":$(this).parent().width(),"margin-top":"-"+(parseInt($(this).parent().height())+2)+"px"});
				}
			});
			
			return;
		}
		
		settings = typeof arguments[0] != undefined ? arguments[0]:$.settings;
		$parent = $(this);
		$(this).data("timer",null);
		$(this).addClass("tinystruct_menu").removeClass("menu").setLevel(0);
		
		$(this).find('> li > a').each(function(i){
			
			if($(this).next("ul").length>0) {
				
				$(this).data("event",settings);
				$(this).siblings().hide();
				if($(this).siblings().level() > 2)
				$(this).siblings().css({"margin-left":$(this).parent().width(),"margin-top":"-"+(parseInt($(this).parent().height())+2)+"px"});

				if($(this).data("event").defaultHandler == 'mouseover') {
					
					$(this).parent().hover(
						function(){
							$(this).addClass("hover");
							
							$(this).find(">ul").slideDown("fast");
							$(this).find(">ul").renderMenu({defaultHandler:"mouseover"});
						},
						function(){
							$(this).find(">ul").slideUp("fast");
							$(this).removeClass("hover");
						}
					);
				}
				else {
					$(this).unbind("click");
					$(this).parent().unbind("hover");
					
					$(this).click(function(event){
						event.preventDefault();

						$(this).next("ul").slideToggle("slow");
						$(this).next("ul").renderMenu({defaultHandler:"click"});
					});
					
					$(this).parent().hover(
						function(){
							$(this).addClass("hover");
							$(this).stop();
						}
						,
						function(){
							$(this).removeClass("hover");
							
							$this = $(this);
							$(this).start(function(){
								$this.find(">ul").slideUp("fast");
								$this.removeClass("hover");
							});
						}
					);
				}
				
			}
		});
		
		$(this).data("rendered",true);
	};
})(jQuery);;
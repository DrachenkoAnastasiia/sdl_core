/**
 * @name MFT.NavigationDestinationPreviousStartingsPointView
 * 
 * @desc Navigation Destination Previous Starting sPoint module visual representation
 * 
 * @category	View
 * @filesource	app/view/navigation/destination/PreviousStartingsPointView.js
 * @version		2.0
 *
 * @author		Maksym Gerashchenko
 */

MFT.NavigationDestinationPreviousStartingsPointView = Em.ContainerView.create(MFT.LoadableView,{
	/** Bind class for visual representation */
	classNameBindings: ['MFT.States.navigation.dest.previousStartingsPoint.active:active_state','MFT.helpMode',],
	
	elementId:		'view_navi_destination_previousStartingsPoint',
	
	classNames:	   ['hidden'],
	
	stateObj: MFT.States.navigation.dest.previousStartingsPoint,
	
	childViews: [
		'backButton',
		'content',
		'heading'
	],
	  
	backButton: MFT.Button.extend({
		classNames:		  ['backButton','button'],		
		action:			  'back',
		target:			  'MFT.States',	
		icon:				'images/media/ico_back.png',	
	}),
	
	/** Upper title*/
	heading: MFT.Label.extend({
		elementId: 'navigation_destination_previousStartingsPoint_title',
		classNames: 'block-header-title',
		contentBinding:  Ember.Binding.oneWay('MFT.locale.label.view_navigation_destination_previousStartingsPoints')
	}),
	
	content: Em.View.create({
		classNames: 'content',
		template: Ember.Handlebars.compile(
				'<div class="label-20 disabled sortby">{{MFT.locale.label.view_navigation_destination_previousStartingsPoint_SortBy}}</div>'+
				'<div class="label-20 disabled date">{{MFT.locale.label.view_navigation_destination_previousStartingsPoint_Date}}</div>'+
				'<div class="label-20 disabled city">{{MFT.locale.label.view_navigation_destination_previousStartingsPoint_City}}</div>'
		)	
	})
});
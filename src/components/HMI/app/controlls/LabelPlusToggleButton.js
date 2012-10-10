/**
 * @name MFT.LablePlusToggleButton
 * 
 * @desc Universal label component with toggle buttons for MFT application
 * 
 * @category	Controlls
 * @filesource	app/controlls/LablePlusToggleButton.js
 * @version		2.0
 *
 * @author		Maksym Getashchenko
 */


MFT.LablePlusToggleButton = Em.ContainerView.extend({
	classNames:			['lableToggleButton'],

	/** Label text */
	labelContent:		'',

	/** Label ico */
	labelIcon:			null,

	/** Index of selected item */
	tButtonValue:		'',

	/** Number of toggle buttons */
	tButtonRange:		'',

	/** Array of labels of toggle buttons */
	tButtonLabels:		[''],

	/** Disable lable */
	labelDisabled:		false,

	/** Disable toggle button */
	tButtonDisabled:	false,
	
	tButtonsClases:		'toggles button',
	
	classContainer:		['toogleButtonContainer'],
	
	childViews: [
		'label',
		'buttonsGroup'
	],

	/** Item lable */
	label: MFT.Label.extend({
		classNames:			['lableToggle'],
		contentBinding:		'parentView.labelContent',
		iconBinding:		'parentView.labelIcon',
		disabledBinding:	'parentView.labelDisabled',
	}),

	/** Item toggle button */
	buttonsGroup: MFT.ButtonsGroup.extend({
		classNameBindings:	'parentView.classContainer',
		valueBinding:		'parentView.tButtonValue',
		rangeBinding: 		'parentView.tButtonRange',
		labelsBinding:		'parentView.tButtonLabels',
		disabledBinding:	'parentView.tButtonDisabled',
		classesBinding: 	'this._parentView.tButtonsClases',
		controllerBinding:	'MFT.SettingsController'
	})
});
I-Logix-RPY-Archive version 8.6.0 C++ 2071424
{ IProject 
	- _id = GUID d64c4fbb-c594-4071-acbc-277bc06d1019;
	- _myState = 8192;
	- _name = "BasicImportExportTestWithPorts";
	- Stereotypes = { IRPYRawContainer 
		- size = 1;
		- value = 
		{ IHandle 
			- _m2Class = "IStereotype";
			- _filename = "$OMROOT\\Profiles\\SysML\\SysMLProfile_rpy\\SysML.sbs";
			- _subsystem = "SysML";
			- _class = "";
			- _name = "SysML";
			- _id = GUID 052b8171-a32b-4f45-a829-5585f79f9deb;
		}
	}
	- _lastID = 2;
	- _UserColors = { IRPYRawContainer 
		- size = 16;
		- value = 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 16777215; 
	}
	- _defaultSubsystem = { ISubsystemHandle 
		- _m2Class = "ISubsystem";
		- _filename = "Default.sbs";
		- _subsystem = "";
		- _class = "";
		- _name = "Default";
		- _id = GUID 66341366-c102-4a08-a2b4-e2f74f8767cb;
	}
	- _component = { IHandle 
		- _m2Class = "IComponent";
		- _filename = "DefaultComponent.cmp";
		- _subsystem = "";
		- _class = "";
		- _name = "DefaultComponent";
		- _id = GUID 4972c2fb-b71a-4ac2-b12d-adfbb00add06;
	}
	- Multiplicities = { IRPYRawContainer 
		- size = 4;
		- value = 
		{ IMultiplicityItem 
			- _name = "1";
			- _count = 5;
		}
		{ IMultiplicityItem 
			- _name = "*";
			- _count = -1;
		}
		{ IMultiplicityItem 
			- _name = "0,1";
			- _count = -1;
		}
		{ IMultiplicityItem 
			- _name = "1..*";
			- _count = -1;
		}
	}
	- Subsystems = { IRPYRawContainer 
		- size = 4;
		- value = 
		{ ISubsystem 
			- fileName = "Default";
			- _id = GUID 66341366-c102-4a08-a2b4-e2f74f8767cb;
		}
		{ IProfile 
			- fileName = "SysML";
			- _persistAs = "$OMROOT\\Profiles\\SysML\\SysMLProfile_rpy";
			- _id = GUID d9689b73-885e-44c4-896b-de43defa0a33;
			- _isReference = 1;
		}
		{ IProfile 
			- fileName = "sm-dm-2.1";
			- _persistAs = "$OMROOT\\Profiles\\sm-dm-2.1";
			- _id = GUID 875eace3-e55f-437d-b02a-3b6dc197d5b6;
			- _name = "Sm-dm-2.1";
			- _isReference = 1;
		}
		{ ISubsystem 
			- fileName = "package_1";
			- _id = GUID db83feca-4579-4a3d-ac42-e254ea6d467a;
		}
	}
	- Diagrams = { IRPYRawContainer 
		- size = 1;
		- value = 
		{ IDiagram 
			- _id = GUID 037b12d0-d599-4ac2-8ca6-9a25438337af;
			- _myState = 8192;
			- _properties = { IPropertyContainer 
				- Subjects = { IRPYRawContainer 
					- size = 1;
					- value = 
					{ IPropertySubject 
						- _Name = "Format";
						- Metaclasses = { IRPYRawContainer 
							- size = 4;
							- value = 
							{ IPropertyMetaclass 
								- _Name = "Class";
								- Properties = { IRPYRawContainer 
									- size = 8;
									- value = 
									{ IProperty 
										- _Name = "DefaultSize";
										- _Value = "0,34,84,148";
										- _Type = String;
									}
									{ IProperty 
										- _Name = "Fill.FillColor";
										- _Value = "255,255,255";
										- _Type = Color;
									}
									{ IProperty 
										- _Name = "Font.Font";
										- _Value = "Tahoma";
										- _Type = String;
									}
									{ IProperty 
										- _Name = "Font.Size";
										- _Value = "8";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Font.Weight@Child.NameCompartment@Name";
										- _Value = "700";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Line.LineColor";
										- _Value = "109,163,217";
										- _Type = Color;
									}
									{ IProperty 
										- _Name = "Line.LineStyle";
										- _Value = "0";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Line.LineWidth";
										- _Value = "1";
										- _Type = Int;
									}
								}
							}
							{ IPropertyMetaclass 
								- _Name = "DiagramFrame";
								- Properties = { IRPYRawContainer 
									- size = 8;
									- value = 
									{ IProperty 
										- _Name = "DefaultSize";
										- _Value = "20,20,590,500";
										- _Type = String;
									}
									{ IProperty 
										- _Name = "Fill.FillColor";
										- _Value = "247,247,247";
										- _Type = Color;
									}
									{ IProperty 
										- _Name = "Fill.Transparent_Fill";
										- _Value = "0";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Font.Font";
										- _Value = "Tahoma";
										- _Type = String;
									}
									{ IProperty 
										- _Name = "Font.Size";
										- _Value = "8";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Font.Weight@Child.NameCompartment@Name";
										- _Value = "700";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Line.LineColor";
										- _Value = "194,192,192";
										- _Type = Color;
									}
									{ IProperty 
										- _Name = "Line.LineWidth";
										- _Value = "1";
										- _Type = Int;
									}
								}
							}
							{ IPropertyMetaclass 
								- _Name = "Flow";
								- Properties = { IRPYRawContainer 
									- size = 5;
									- value = 
									{ IProperty 
										- _Name = "Font.Font";
										- _Value = "Tahoma";
										- _Type = String;
									}
									{ IProperty 
										- _Name = "Font.Size";
										- _Value = "8";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Line.LineColor";
										- _Value = "128,128,128";
										- _Type = Color;
									}
									{ IProperty 
										- _Name = "Line.LineStyle";
										- _Value = "2";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Line.LineWidth";
										- _Value = "0";
										- _Type = Int;
									}
								}
							}
							{ IPropertyMetaclass 
								- _Name = "Object";
								- Properties = { IRPYRawContainer 
									- size = 9;
									- value = 
									{ IProperty 
										- _Name = "DefaultSize";
										- _Value = "0,34,84,148";
										- _Type = String;
									}
									{ IProperty 
										- _Name = "Fill.FillColor";
										- _Value = "255,255,255";
										- _Type = Color;
									}
									{ IProperty 
										- _Name = "Font.Font";
										- _Value = "Tahoma";
										- _Type = String;
									}
									{ IProperty 
										- _Name = "Font.Size";
										- _Value = "8";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Font.Underline@Child.NameCompartment@Name";
										- _Value = "0";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Font.Weight@Child.NameCompartment@Name";
										- _Value = "700";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Line.LineColor";
										- _Value = "109,163,217";
										- _Type = Color;
									}
									{ IProperty 
										- _Name = "Line.LineStyle";
										- _Value = "0";
										- _Type = Int;
									}
									{ IProperty 
										- _Name = "Line.LineWidth";
										- _Value = "1";
										- _Type = Int;
									}
								}
							}
						}
					}
				}
			}
			- _name = "Model1";
			- Stereotypes = { IRPYRawContainer 
				- size = 1;
				- value = 
				{ IHandle 
					- _m2Class = "IStereotype";
					- _filename = "$OMROOT\\Profiles\\SysML\\SysMLProfile_rpy\\SysML.sbs";
					- _subsystem = "SysML::Diagrams";
					- _class = "";
					- _name = "Block Definition Diagram";
					- _id = GUID 6c142614-3349-49e9-8c6b-0236be5f6b61;
				}
			}
			- _lastModifiedTime = "6.6.2012::21:55:41";
			- _graphicChart = { CGIClassChart 
				- _id = GUID d64a3011-864e-4f86-bee3-299866b2b3c1;
				- m_type = 0;
				- m_pModelObject = { IHandle 
					- _m2Class = "IDiagram";
					- _id = GUID 037b12d0-d599-4ac2-8ca6-9a25438337af;
				}
				- m_pParent = ;
				- m_name = { CGIText 
					- m_str = "";
					- m_style = "Arial" 10 0 0 0 1 ;
					- m_color = { IColor 
						- m_fgColor = 0;
						- m_bgColor = 0;
						- m_bgFlag = 0;
					}
					- m_position = 1 0 0  ;
					- m_nIdent = 0;
					- m_bImplicitSetRectPoints = 0;
					- m_nOrientationCtrlPt = 8;
				}
				- m_drawBehavior = 0;
				- m_bIsPreferencesInitialized = 0;
				- elementList = 15;
				{ CGIClass 
					- _id = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
					- m_type = 78;
					- m_pModelObject = { IHandle 
						- _m2Class = "IClass";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "";
						- _name = "TopLevel";
						- _id = GUID bff8ecc4-2379-4a46-bb4e-d400a84b828c;
					}
					- m_pParent = ;
					- m_name = { CGIText 
						- m_str = "TopLevel";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 5;
					}
					- m_drawBehavior = 0;
					- m_bIsPreferencesInitialized = 0;
					- m_AdditionalLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 1;
					}
					- m_polygon = 0 ;
					- m_nNameFormat = 0;
					- m_nIsNameFormat = 0;
					- Compartments = { IRPYRawContainer 
						- size = 0;
					}
					- Attrs = { IRPYRawContainer 
						- size = 0;
					}
					- Operations = { IRPYRawContainer 
						- size = 0;
					}
				}
				{ CGIDiagramFrame 
					- _id = GUID 59b42bad-dac1-4964-a5db-1ea1b796b2d2;
					- m_type = 203;
					- m_pModelObject = { IHandle 
						- _m2Class = "";
					}
					- m_pParent = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
					- m_name = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 8;
					}
					- m_drawBehavior = 4096;
					- m_transform = 2.63889 0 0 3.63636 20 20 ;
					- m_bIsPreferencesInitialized = 1;
					- m_AdditionalLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 1;
					}
					- m_polygon = 4 0 0  0 132  216 132  216 0  ;
					- m_nNameFormat = 0;
					- m_nIsNameFormat = 0;
					- Compartments = { IRPYRawContainer 
						- size = 0;
					}
				}
				{ CGIObjectInstance 
					- _id = GUID 6099112e-ce80-4620-970c-8ec03ea0a9b8;
					- m_type = 106;
					- m_pModelObject = { IHandle 
						- _m2Class = "IPart";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M";
						- _name = "k1";
						- _id = GUID 1a2f45d0-5280-4d84-8eaf-4303f5a25158;
					}
					- m_pParent = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
					- m_name = { CGIText 
						- m_str = "M.k1:M1";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 5;
					}
					- m_drawBehavior = 2824;
					- m_transform = 0.113314 0 0 0.030303 129.773 30.0303 ;
					- m_bIsPreferencesInitialized = 1;
					- m_AdditionalLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 1;
					}
					- m_polygon = 4 2 329  2 1451  1061 1451  1061 329  ;
					- m_nNameFormat = 0;
					- m_nIsNameFormat = 0;
					- frameset = "<frameset rows=>";
					- Compartments = { IRPYRawContainer 
						- size = 0;
					}
					- Attrs = { IRPYRawContainer 
						- size = 0;
					}
					- Operations = { IRPYRawContainer 
						- size = 0;
					}
					- m_multiplicity = { CGIText 
						- m_str = "1";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIObjectInstance 
					- _id = GUID 7a91e80e-f0c5-442b-88df-8d74c46448e1;
					- m_type = 106;
					- m_pModelObject = { IHandle 
						- _m2Class = "IPart";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M";
						- _name = "k2";
						- _id = GUID c47c5f00-a0a2-4985-a7d6-881e676b6a81;
					}
					- m_pParent = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
					- m_name = { CGIText 
						- m_str = "M.k2:M2";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 5;
					}
					- m_drawBehavior = 2824;
					- m_transform = 0.113314 0 0 0.030303 39.7734 107.03 ;
					- m_bIsPreferencesInitialized = 1;
					- m_AdditionalLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 1;
					}
					- m_polygon = 4 2 329  2 1451  1061 1451  1061 329  ;
					- m_nNameFormat = 0;
					- m_nIsNameFormat = 0;
					- frameset = "<frameset rows=>";
					- Compartments = { IRPYRawContainer 
						- size = 0;
					}
					- Attrs = { IRPYRawContainer 
						- size = 0;
					}
					- Operations = { IRPYRawContainer 
						- size = 0;
					}
					- m_multiplicity = { CGIText 
						- m_str = "1";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIClass 
					- _id = GUID c2abae75-c9ea-4796-907b-342706251570;
					- m_type = 87;
					- m_pModelObject = { IHandle 
						- _m2Class = "IClass";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M";
						- _name = "M1";
						- _id = GUID fff2e05a-168f-4246-9a76-52b14647f403;
					}
					- m_pParent = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
					- m_name = { CGIText 
						- m_str = "M::M1";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 5;
					}
					- m_drawBehavior = 2056;
					- m_transform = 0.113314 0 0 0.030303 219.773 107.03 ;
					- m_bIsPreferencesInitialized = 1;
					- m_AdditionalLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 1;
					}
					- m_polygon = 4 2 329  2 1451  1061 1451  1061 329  ;
					- m_nNameFormat = 0;
					- m_nIsNameFormat = 0;
					- frameset = "<frameset rows=>";
					- Compartments = { IRPYRawContainer 
						- size = 0;
					}
					- Attrs = { IRPYRawContainer 
						- size = 0;
					}
					- Operations = { IRPYRawContainer 
						- size = 0;
					}
				}
				{ CGIClass 
					- _id = GUID ddf682ee-bfd8-47c4-9dcb-2f5c29fddf2f;
					- m_type = 87;
					- m_pModelObject = { IHandle 
						- _m2Class = "IClass";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M";
						- _name = "M2";
						- _id = GUID bd49b29d-1448-45dc-9b31-d2f98d56c43a;
					}
					- m_pParent = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
					- m_name = { CGIText 
						- m_str = "M::M2";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 5;
					}
					- m_drawBehavior = 2056;
					- m_transform = 0.113314 0 0 0.030303 219.773 184.03 ;
					- m_bIsPreferencesInitialized = 1;
					- m_AdditionalLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 1;
					}
					- m_polygon = 4 2 329  2 1451  1061 1451  1061 329  ;
					- m_nNameFormat = 0;
					- m_nIsNameFormat = 0;
					- frameset = "<frameset rows=>";
					- Compartments = { IRPYRawContainer 
						- size = 0;
					}
					- Attrs = { IRPYRawContainer 
						- size = 0;
					}
					- Operations = { IRPYRawContainer 
						- size = 0;
					}
				}
				{ CGIPortConnector 
					- _id = GUID 4b31ee0f-2e4d-457d-afd1-646bc0e0a7b8;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M2";
						- _name = "p2a";
						- _id = GUID 7bb232fc-44b6-48c4-bf9e-f23b357de119;
					}
					- m_pParent = GUID ddf682ee-bfd8-47c4-9dcb-2f5c29fddf2f;
					- m_name = { CGIText 
						- m_str = "p2a";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 2;
					}
					- m_drawBehavior = 4096;
					- m_transform = -0 -32.9999 8.82497 -0 -342.175 1846.61 ;
					- m_bIsPreferencesInitialized = 1;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIPortConnector 
					- _id = GUID c5dddc70-f9cf-4353-84f3-664f20d49e24;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M2";
						- _name = "p2a";
						- _id = GUID 7bb232fc-44b6-48c4-bf9e-f23b357de119;
					}
					- m_pParent = GUID 7a91e80e-f0c5-442b-88df-8d74c46448e1;
					- m_name = { CGIText 
						- m_str = "p2a";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 2;
					}
					- m_drawBehavior = 0;
					- m_transform = -9.08243e-006 -32.9999 8.82497 -3.39625e-005 -343.575 1681.77 ;
					- m_bIsPreferencesInitialized = 0;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIPortConnector 
					- _id = GUID 9cd1dbbd-48be-4fee-b0c7-32dbab0ccb20;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M2";
						- _name = "p2b";
						- _id = GUID 0b75a23a-14dd-4c23-8f43-a1ceb6924bf6;
					}
					- m_pParent = GUID ddf682ee-bfd8-47c4-9dcb-2f5c29fddf2f;
					- m_name = { CGIText 
						- m_str = "p2b";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 2;
					}
					- m_drawBehavior = 4096;
					- m_transform = -2.93513e-005 -32.9999 8.82497 -0.000109756 -343.574 2063.32 ;
					- m_bIsPreferencesInitialized = 1;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIPortConnector 
					- _id = GUID a3963d4c-d341-4cff-a0f5-a39180ff3d28;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M2";
						- _name = "p2b";
						- _id = GUID 0b75a23a-14dd-4c23-8f43-a1ceb6924bf6;
					}
					- m_pParent = GUID 7a91e80e-f0c5-442b-88df-8d74c46448e1;
					- m_name = { CGIText 
						- m_str = "p2b";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 2;
					}
					- m_drawBehavior = 0;
					- m_transform = -2.93513e-005 -32.9999 8.82497 -0.000109756 -343.574 2063.32 ;
					- m_bIsPreferencesInitialized = 0;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIPortConnector 
					- _id = GUID 6545d1fc-9c7f-4a1f-8946-98bea9050092;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M1";
						- _name = "p1a";
						- _id = GUID 16b7ddaa-6d51-434d-8566-564628902614;
					}
					- m_pParent = GUID 6099112e-ce80-4620-970c-8ec03ea0a9b8;
					- m_name = { CGIText 
						- m_str = "p1a";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 0;
					}
					- m_drawBehavior = 4096;
					- m_transform = 0 32.9999 -8.82497 0 1403.78 -406.676 ;
					- m_bIsPreferencesInitialized = 1;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIPortConnector 
					- _id = GUID 5a432de5-49ac-4a99-ab23-078e2e158659;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M1";
						- _name = "p1a";
						- _id = GUID 16b7ddaa-6d51-434d-8566-564628902614;
					}
					- m_pParent = GUID c2abae75-c9ea-4796-907b-342706251570;
					- m_name = { CGIText 
						- m_str = "p1a";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 0;
					}
					- m_drawBehavior = 0;
					- m_transform = 0 32.9999 -8.82497 0 1403.78 -303.045 ;
					- m_bIsPreferencesInitialized = 0;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIPortConnector 
					- _id = GUID c702eb27-d72f-47a1-8bd8-857497919a37;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M1";
						- _name = "p1b";
						- _id = GUID 437271cd-5f5c-4325-bf65-552f7bc349da;
					}
					- m_pParent = GUID c2abae75-c9ea-4796-907b-342706251570;
					- m_name = { CGIText 
						- m_str = "p1b";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 0;
					}
					- m_drawBehavior = 4096;
					- m_transform = 0 32.9999 -8.82497 0 1403.78 -24.1812 ;
					- m_bIsPreferencesInitialized = 1;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIPortConnector 
					- _id = GUID 2a2fbdbc-fe8f-4a3b-b654-0af8a3e4f492;
					- m_type = 201;
					- m_pModelObject = { IHandle 
						- _m2Class = "ISysMLPort";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "M::M1";
						- _name = "p1b";
						- _id = GUID 437271cd-5f5c-4325-bf65-552f7bc349da;
					}
					- m_pParent = GUID 6099112e-ce80-4620-970c-8ec03ea0a9b8;
					- m_name = { CGIText 
						- m_str = "p1b";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nVerticalSpacing = -19;
						- m_nOrientationCtrlPt = 0;
					}
					- m_drawBehavior = 0;
					- m_transform = 9.08243e-006 32.9999 -8.82497 3.39625e-005 1405.18 -11.036 ;
					- m_bIsPreferencesInitialized = 0;
					- m_position = 4 0 0  0 73  68 73  68 0  ;
					- m_pInheritsFrom = { IHandle 
						- _m2Class = "";
					}
					- m_nInheritanceMask = 0;
					- m_SubType = 0;
					- m_providedInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = 67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
					- m_requiredInterfaceLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nHorizontalSpacing = -67;
						- m_nVerticalSpacing = -25;
						- m_nOrientationCtrlPt = 8;
					}
				}
				{ CGIClass 
					- _id = GUID 837b7e2e-3687-4ca4-83fe-ebdf72f55ee3;
					- m_type = 87;
					- m_pModelObject = { IHandle 
						- _m2Class = "IClass";
						- _filename = "Default.sbs";
						- _subsystem = "Default";
						- _class = "";
						- _name = "M";
						- _id = GUID 0208a299-1530-4e1f-aa76-03c71b1c38f2;
					}
					- m_pParent = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
					- m_name = { CGIText 
						- m_str = "M";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 5;
					}
					- m_drawBehavior = 2056;
					- m_transform = 0.113314 0 0 0.030303 39.7734 184.03 ;
					- m_bIsPreferencesInitialized = 1;
					- m_AdditionalLabel = { CGIText 
						- m_str = "";
						- m_style = "Arial" 10 0 0 0 1 ;
						- m_color = { IColor 
							- m_fgColor = 0;
							- m_bgColor = 0;
							- m_bgFlag = 0;
						}
						- m_position = 1 0 0  ;
						- m_nIdent = 0;
						- m_bImplicitSetRectPoints = 0;
						- m_nOrientationCtrlPt = 1;
					}
					- m_polygon = 4 2 329  2 1451  1061 1451  1061 329  ;
					- m_nNameFormat = 0;
					- m_nIsNameFormat = 0;
					- frameset = "<frameset rows=>";
					- Compartments = { IRPYRawContainer 
						- size = 0;
					}
					- Attrs = { IRPYRawContainer 
						- size = 0;
					}
					- Operations = { IRPYRawContainer 
						- size = 0;
					}
				}
				
				- m_access = 'Z';
				- m_modified = 'N';
				- m_fileVersion = "";
				- m_nModifyDate = 0;
				- m_nCreateDate = 0;
				- m_creator = "";
				- m_bScaleWithZoom = 1;
				- m_arrowStyle = 'S';
				- m_pRoot = GUID 1d9aa9a8-167c-4c3c-9b4b-3486ec11bb56;
				- m_currentLeftTop = 0 0 ;
				- m_currentRightBottom = 0 0 ;
			}
			- _defaultSubsystem = { IHandle 
				- _m2Class = "ISubsystem";
				- _filename = "Default.sbs";
				- _subsystem = "";
				- _class = "";
				- _name = "Default";
				- _id = GUID 66341366-c102-4a08-a2b4-e2f74f8767cb;
			}
		}
	}
	- Components = { IRPYRawContainer 
		- size = 1;
		- value = 
		{ IComponent 
			- fileName = "DefaultComponent";
			- _id = GUID 4972c2fb-b71a-4ac2-b12d-adfbb00add06;
		}
	}
}


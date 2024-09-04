exclude :test_change_class_name, "[TypeError] exception expected, not #<NoMethodError: undefined method `_load' for TestMarshal::C3:Class>."
exclude :test_change_struct, "[TypeError] exception expected, not #<NoMethodError: undefined method `intern' for nil:NilClass>."
exclude :test_class_ivar, "TypeError expected but nothing was raised."
exclude :test_class_nonascii, "Encoding::CompatibilityError: incompatible character encodings: UTF-8 and ISO-8859-1"
exclude :test_continuation, "Expected Exception(RuntimeError) was raised, but the message doesn't match. Expected /Marshal\\.dump reentered at marshal_dump/ to match \"Continuations are unsupported on TruffleRuby\"."
exclude :test_inconsistent_struct, "[TypeError] exception expected, not #<NoMethodError: undefined method `members' for TestMarshal::StructOrNot:Class>."
exclude :test_load_range_as_struct, "GH-6832."
exclude :test_marshal_complex, "ArgumentError expected but nothing was raised."
exclude :test_marshal_dump_adding_instance_variable, "Exception(RuntimeError) with message matches to /instance variable added/."
exclude :test_marshal_dump_ivar, "FrozenError expected but nothing was raised."
exclude :test_marshal_dump_recursion, "Exception(RuntimeError) with message matches to /same class instance/."
exclude :test_marshal_dump_removing_instance_variable, "Exception(RuntimeError) with message matches to /instance variable removed/."
exclude :test_marshal_honor_post_proc_value_for_link, "<[\"X\", \"X\"]> expected but was <[\"X\", \"x\"]>."
exclude :test_marshal_load_extended_class_crash, "Exception(ArgumentError) with message matches to /undefined/."
exclude :test_marshal_load_ivar, "TestMarshal::LoadData."
exclude :test_marshal_load_r_prepare_reference_crash, "Exception(ArgumentError) with message matches to /bad link/."
exclude :test_marshal_nameerror, "<:unknown_method> expected but was <nil>."
exclude :test_marshal_rational, "ArgumentError expected but nothing was raised."
exclude :test_marshal_string_encoding, "<[\"foo\", \"bar\", \"bar\"]> expected but was <[\"foo\", \"bar\", \"EUC-JP\"]>."
exclude :test_marshal_with_ruby2_keywords_hash, "ArgumentError: wrong number of arguments (given 1, expected 0)"
exclude :test_modify_array_during_dump, "RuntimeError expected but nothing was raised."
exclude :test_module_ivar, "TypeError expected but nothing was raised."
exclude :test_no_internal_ids, "Expected \"ERROR: Invalid argument --debug=frozen-string-literal specified. Invalid boolean option value 'frozen-string-literal'. The value of the option must be 'true' or 'false'.\\n\" to be empty."
exclude :test_object_prepend, "<[MarshalTestLib::Mod1,"
exclude :test_range_cyclic, "ArgumentError: dump format error (unlinked)"
exclude :test_range_subclass, "ArgumentError: wrong number of arguments (given 4, expected 2..3)"
exclude :test_regexp, "<\"/\\xE3\\x81\\x82/\"> expected but was <\"/\\u3042/\">."
exclude :test_regexp2, "RegexpError: too short escape sequence: /\\u/"
exclude :test_regexp_subclass, "TypeError: no implicit conversion of Integer into String"
exclude :test_singleton, "TypeError expected but nothing was raised."
exclude :test_struct_invalid_members, "TypeError expected but nothing was raised."
exclude :test_symlink_in_ivar, "ArgumentError: bad symbol"
exclude :test_time_subclass, "ArgumentError: year too big to marshal: 10"
exclude :test_unloadable_data, "Exception(TypeError) with message matches to /Unloadable\\u{23F0 23F3}/."
exclude :test_unloadable_userdef, "Exception(TypeError) with message matches to /Userdef\\u{23F0 23F3}/."
exclude :test_unloadable_usrmarshal, "Exception(TypeError) with message matches to /UsrMarshal\\u{23F0 23F3}/."

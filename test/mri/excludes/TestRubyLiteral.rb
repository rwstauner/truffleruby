exclude :test_integer, "needs investigation"
exclude :test_string, "needs investigation"
exclude :test_hash_value_omission, "NameError: undefined local variable or method `FOO' for #<TestRubyLiteral:0x2914b8>"
exclude :test_hash_duplicated_key, "duplicated literal key."
exclude :test_float, "_1 inside eval, see https://github.com/ruby/prism/issues/2275"
exclude :test_dregexp, "prism missing regexp encoding flags"

<?php
/**
 * This file has been auto generated
 * Do not change it
 */

namespace Test\Base;

class MapIterator extends \ArrayIterator
{
    /**
     * @var callable
     */
    private $callback;

    public function __construct($value, callable $callback)
    {
        parent::__construct($value);
        $this->callback = $callback;
    }

    public function current()
    {
        $value = parent::current();
        $key = parent::key();
        return call_user_func($this->callback, $value, $key);
    }
}
